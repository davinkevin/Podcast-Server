package lan.dk.podcastserver.manager.worker.downloader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.TypeRef;
import javaslang.collection.HashSet;
import javaslang.collection.Set;
import javaslang.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.worker.updater.SixPlayUpdater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 22/03/2017 for Podcast Server
 */
@Slf4j
@Scope("prototype")
@Component("SixPlayDownloader")
public class SixPlayDownloader extends M3U8Downloader {

    private static final TypeRef<Set<M6PlayItem>> TYPE_ITEMS = new TypeRef<Set<M6PlayItem>>() {};
    private static final String ITEMS_EXTRACTOR = "video.currentVideo.clips[*].assets[*]";

    private final HtmlService htmlService;
    private final JsonService jsonService;

    private Option<String> url = Option.none();

    public SixPlayDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, UrlService urlService, M3U8Service m3U8Service, FfmpegService ffmpegService, ProcessService processService, HtmlService htmlService, JsonService jsonService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService, urlService, m3U8Service, ffmpegService, processService);
        this.htmlService = htmlService;
        this.jsonService = jsonService;
    }

    @Override
    public String getItemUrl(Item item) {

        if (nonNull(this.item) && !this.item.equals(item)) {
            return item.getUrl();
        }

        url = url.orElse(() -> htmlService.get(item.getUrl()).map(d -> this.extractUrl(d.select("script"))));

        return url.getOrElseThrow(() -> new RuntimeException("Url not found for " + item.getUrl()));
    }

    private String extractUrl(Elements script) {
        Set<M6PlayItem> items = extractJson(script)
                .map(v -> JsonService.to(ITEMS_EXTRACTOR, TYPE_ITEMS).apply(v))
                .getOrElse(HashSet.empty());

        return items
                .find(i -> "usp_hls_h264".equals(i.getType()))
                .orElse(() -> items.find(i -> "hq".equalsIgnoreCase(i.getVideo_quality())))
                .orElse(() -> items.find(i -> "hd".equalsIgnoreCase(i.getVideo_quality())))
                .orElse(() -> items.find(i -> "sd".equalsIgnoreCase(i.getVideo_quality())))
                .map(M6PlayItem::getFull_physical_path)
                .getOrElseThrow(() -> new RuntimeException("No Stream found for item " + this.item.getTitle()));
    }

    private Option<DocumentContext> extractJson(Elements elements) {
        return SixPlayUpdater.getRoot6Play(elements)
                .map(jsonService::parse);
    }

    @Override
    public Integer compatibility(String url) {
        return SixPlayUpdater.isFrom6Play(url);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class M6PlayItem {
        @Setter @Getter private String video_quality;
        @Setter @Getter private String full_physical_path;
        @Setter @Getter private String type;
    }
}
