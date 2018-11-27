package lan.dk.podcastserver.manager.worker.francetv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davinkevin.podcastserver.manager.worker.francetv.FranceTvUpdater;
import com.github.davinkevin.podcastserver.service.HtmlService;
import io.vavr.collection.List;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.manager.worker.Extractor;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static io.vavr.API.*;

/**
 * Created by kevin on 24/12/2017
 */
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class FranceTvExtractor implements Extractor {

    private static final String CATALOG_URL = "https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s";

    private final HtmlService htmlService;
    private final JsonService jsonService;

    @Override
    public DownloadingItem extract(Item item) {
        return htmlService.get(item.getUrl())
                .map(d -> d.select("#player").first())
                .map(e -> e.attr("data-main-video"))
                .flatMap(id -> jsonService.parseUrl(String.format(CATALOG_URL, id)))
                .map(JsonService.to(FranceTvItem.class))
                .map(FranceTvItem::getUrl)
                .map(url -> Tuple(url, getFileName(item)))
                .map(info -> DownloadingItem.builder()
                        .filename(info._2())
                        .urls(List(info._1()))
                        .item(item)
                    .build())
                .getOrElseThrow(() -> new RuntimeException("Error during extraction of FranceTV item"));
    }


    @Override
    public Integer compatibility(String url) {
        return FranceTvUpdater.Companion.isFromFranceTv(url);
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
