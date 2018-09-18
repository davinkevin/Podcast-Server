package lan.dk.podcastserver.manager.worker.dailymotion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davinkevin.podcastserver.service.M3U8Service;
import com.github.davinkevin.podcastserver.service.UrlService;
import com.mashape.unirest.http.HttpResponse;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.manager.worker.Extractor;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static io.vavr.API.*;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Created by kevin on 24/12/2017
 */
@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class DailymotionExtractor implements Extractor {

    private final JsonService jsonService;
    private final UrlService urlService;
    private final M3U8Service m3U8Service;

    @Override
    public DownloadingItem extract(Item item) {
        return Try(() -> urlService
                .get(item.getUrl())
                .asString()
        )
                .map(HttpResponse::getBody)
                .toOption()
                .flatMap(DailymotionExtractor::getPlayerConfig)
                .map(jsonService::parse)
                .map(JsonService.to("metadata", DailymotionMetadata.class))
                .map(DailymotionMetadata::getUrl)
                .filter(Objects::nonNull)
                .map(m3U8Service::getM3U8UrlFormMultiStreamFile)
                .map(this::removeHash)
                .map(url -> Tuple(url, getFileName(item)))
                .map(info -> DownloadingItem.builder()
                            .item(item)
                            .urls(List(info._1()))
                            .filename(info._2())
                        .build()
                )
                .getOrElseThrow(() -> new RuntimeException("Error during Daylimotion extraction of " + item.getTitle() + " with url " + item.getUrl()));
    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("dailymotion.com/video") ? 1 : Integer.MAX_VALUE;
    }

    private String removeHash(String s) {
        return StringUtils.substringBefore(s, "#");
    }

    private static Option<String> getPlayerConfig(String page) {
        String startToken = "var __PLAYER_CONFIG__ = ";
        String endToken = "};";

        if (!page.contains(startToken) || !page.contains(endToken)) {
            log.error("Structure of Dailymotion page changed");
            return None();
        }

        int begin = page.indexOf(startToken);
        int end = page.indexOf(endToken, begin);
        return Option(page.substring(begin + startToken.length() - 1, end + 1));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DailymotionMetadata {
        @JsonProperty("stream_chromecast_url") @Getter @Setter String url;
    }
}
