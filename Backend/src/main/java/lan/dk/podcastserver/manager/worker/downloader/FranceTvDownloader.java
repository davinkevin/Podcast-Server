package lan.dk.podcastserver.manager.worker.downloader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.API;
import io.vavr.Lazy;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.worker.updater.FranceTvUpdater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static io.vavr.API.Option;

/**
 * Created by kevin on 01/07/2017.
 */
@Slf4j
@Scope("prototype")
@Component("FranceTvDownloader")
public class FranceTvDownloader extends M3U8Downloader {

    private static final String CATALOG_URL = "https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s";

    private final HtmlService htmlService;
    private final JsonService jsonService;

    private final Lazy<Option<String>> url = API.Lazy(this::_getItemUrl);

    public FranceTvDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, UrlService urlService, M3U8Service m3U8Service, FfmpegService ffmpegService, ProcessService processService, HtmlService htmlService, JsonService jsonService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService, urlService, m3U8Service, ffmpegService, processService);
        this.htmlService = htmlService;
        this.jsonService = jsonService;
    }

    @Override
    public String getFileName(Item item) {
        return Option(item.getUrl())
                .map(s -> StringUtils.substringAfterLast(s, "/"))
                .map(s -> StringUtils.substringBeforeLast(s, "?"))
                .map(FilenameUtils::getBaseName)
                .map(s -> s + ".mp4")
                .getOrElse("");
    }

    @Override
    public String getItemUrl(Item item) {
        return url.get().getOrElseThrow(() -> new RuntimeException("Url not found for " + item.getUrl()));
    }

    private Option<String> _getItemUrl() {
        return htmlService.get(this.item.getUrl())
                .map(d -> d.select("#player").first())
                .map(e -> e.attr("data-main-video"))
                .flatMap(id -> jsonService.parseUrl(String.format(CATALOG_URL, id)))
                .map(JsonService.to(FranceTvItem.class))
                .map(FranceTvItem::getUrl);
    }

    @Override
    public Integer compatibility(String url) {
        return FranceTvUpdater.isFromFranceTv(url);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FranceTvItem {

        @Setter @Getter private List<FranceTvItem.Video> videos = List.empty();

        public String getUrl() {
            return videos
                    .find(v -> "hls_v5_os".equals(v.getFormat()))
                    .orElse(videos.find(v -> "m3u8-download".equals(v.getFormat())))
                    .orElse(videos.find(v -> v.getSecureUrl().contains("m3u8")))
                    .flatMap(v -> Option(v.getSecureUrl()).orElse(Option(v.getUrl())))
                    .getOrElseThrow(() -> new RuntimeException("No video found in this FranceTvItem " + this));

        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class Video {
            @Getter @Setter private String format;
            @Getter @Setter private String url;
            @JsonProperty("url_secure") @Getter @Setter private String secureUrl;
        }
    }

}
