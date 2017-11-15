package lan.dk.podcastserver.manager.worker.downloader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.TypeRef;
import com.mashape.unirest.http.HttpResponse;
import io.vavr.Lazy;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.updater.SixPlayUpdater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.vavr.API.*;

/**
 * Created by kevin on 22/03/2017 for Podcast Server
 */
@Slf4j
@Scope("prototype")
@Component("SixPlayDownloader")
public class SixPlayDownloader extends M3U8Downloader {

    private static final TypeRef<Set<M6PlayItem>> TYPE_ITEMS = new TypeRef<Set<M6PlayItem>>(){};
    private static final String ITEMS_EXTRACTOR = "mainStoreState.video.currentVideo.clips[*]";

    private final HtmlService htmlService;
    private final JsonService jsonService;

    private final Lazy<Set<String>> urls = Lazy(this::_getItemUrls);
    private Double globalDuration = 0d;
    private Double alreadyDoneDuration = 0d;

    public SixPlayDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, UrlService urlService, M3U8Service m3U8Service, FfmpegService ffmpegService, ProcessService processService, HtmlService htmlService, JsonService jsonService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService, urlService, m3U8Service, ffmpegService, processService);
        this.htmlService = htmlService;
        this.jsonService = jsonService;
    }

    @Override
    public Item download() {
        if (urls.get().length() == 1) {
            return super.download();
        }

        target = getTargetFile(item);

        globalDuration = urls.get()
                .map(url -> ffmpegService.getDurationOf(url, withUserAgent()))
                .sum()
                .doubleValue();

        Set<Path> multiFiles = urls.get().map(this::download);

        ffmpegService.concat(target, multiFiles.toJavaArray(Path.class));

        multiFiles.forEach(v -> Try.run(() -> Files.delete(v)));

        if (item.getStatus() == Status.STARTED)
            finishDownload();

        return item;
    }

    private Path download(String url) {
        log.debug("Download {} from {}", url, item.getTitle());

        Double duration = ffmpegService.getDurationOf(url, withUserAgent());

        Path subTarget = this.generateTempFileNextTo(target);

        FFmpegBuilder command = new FFmpegBuilder()
                .setUserAgent(withUserAgent())
                .addInput(url)
                .addOutput(subTarget.toAbsolutePath().toString())
                .setFormat("mp4")
                .setAudioBitStreamFilter(FfmpegService.AUDIO_BITSTREAM_FILTER_AAC_ADTSTOASC)
                .setVideoCodec(FfmpegService.CODEC_COPY)
                .setAudioCodec(FfmpegService.CODEC_COPY)
                .done();


        process = ffmpegService.download(url, command, handleProgression(alreadyDoneDuration, globalDuration));

        processService.waitFor(process);

        alreadyDoneDuration += duration;

        return subTarget;
    }

    @Override
    public String getFileName(Item item) {
        // http://www.6play.fr/le-message-de-madenian-et-vdb-p_6730/mm-vdb-22-06-c_11699574
        return Option(item.getUrl())
                .map(s -> StringUtils.substringAfterLast(s, "/"))
                .map(s -> StringUtils.substringBeforeLast(s, "?"))
                .map(FilenameUtils::getBaseName)
                .map(s -> s + ".mp4")
                .getOrElse("");
    }

    @Override
    public String getItemUrl(Item item) {
        return urls.get().get();
    }

    private Set<String> _getItemUrls() {
        return htmlService.get(item.getUrl())
                .map(d -> this.extractUrl(d.select("script")))
                .getOrElse(HashSet::empty);
    }

    private Set<String> extractUrl(Elements script) {
        Set<M6PlayItem> items = extractJson(script)
                .map(JsonService.to(ITEMS_EXTRACTOR, TYPE_ITEMS))
                .getOrElse(HashSet::empty);

        return items
                .flatMap(this::keepBestQuality)
                .map(M6PlayAssets::getFull_physical_path);
    }

    private Option<M6PlayAssets> keepBestQuality(M6PlayItem item) {
        Set<M6PlayAssets> assets = item.getAssets();

        return assets
                .find(i -> "sd3-ism".equalsIgnoreCase(i.getVideo_quality()))
                .flatMap(this::transformSd3Url)
                .orElse(() -> assets.find(i -> "usp_hls_h264".equalsIgnoreCase(i.getType())))
                .orElse(() -> assets.find(i -> "hq".equalsIgnoreCase(i.getVideo_quality())))
                .orElse(() -> assets.find(i -> "hd".equalsIgnoreCase(i.getVideo_quality())))
                .orElse(() -> assets.find(i -> "sd".equalsIgnoreCase(i.getVideo_quality())));
    }

    private Option<M6PlayAssets> transformSd3Url(M6PlayAssets asset) {
        String modifiedUrl = asset.getFull_physical_path().replaceAll("/([^/]+)\\.ism/[^/]*\\.m3u8", "/$1.ism/$1.m3u8");

        return Try(() -> urlService.get(modifiedUrl)
                .header(UrlService.USER_AGENT_KEY, UrlService.USER_AGENT_MOBILE)
                .asString())
                .map(HttpResponse::getRawBody)
                .flatMap(is -> m3U8Service.findBestQuality(is).toTry())
                .map(url -> urlService.addDomainIfRelative(modifiedUrl, url))
                .map(url -> new M6PlayAssets(asset.getVideo_quality(), url, asset.getType()))
                .toOption();
    }

    private Option<DocumentContext> extractJson(Elements elements) {
        return SixPlayUpdater.getRoot6Play(elements).map(jsonService::parse);
    }

    @Override
    public Integer compatibility(String url) {
        return SixPlayUpdater.isFrom6Play(url);
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class M6PlayItem {
        @Getter @Setter private Set<M6PlayAssets> assets;
    }

    @AllArgsConstructor @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class M6PlayAssets {
        @Setter @Getter private String video_quality;
        @Setter @Getter private String full_physical_path;
        @Setter @Getter private String type;
    }

}
