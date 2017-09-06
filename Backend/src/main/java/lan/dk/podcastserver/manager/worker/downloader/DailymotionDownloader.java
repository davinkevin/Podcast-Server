package lan.dk.podcastserver.manager.worker.downloader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mashape.unirest.http.HttpResponse;
import io.vavr.Lazy;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static io.vavr.API.*;

/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Slf4j
@Scope("prototype")
@Component("DailymotionDownloader")
public class DailymotionDownloader extends M3U8Downloader {

    private final Lazy<Option<String>> url = Lazy(this::_getItemUrl);
    private final JsonService jsonService;

    public DailymotionDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, UrlService urlService, M3U8Service m3U8Service, FfmpegService ffmpegService, ProcessService processService, JsonService jsonService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService, urlService, m3U8Service, ffmpegService, processService);
        this.jsonService = jsonService;
    }

    public String getItemUrl(Item item) {

        if (!Objects.equals(this.item, item)) {
            return super.getItemUrl(item);
        }

        return url.get().getOrElseThrow(() -> new RuntimeException("Url not found for " + item.getUrl()));
    }

    private Option<String> _getItemUrl() {
        return Try(() -> urlService
                .get(item.getUrl())
                .asString()
        )
                .map(HttpResponse::getBody)
                .toOption()
                .flatMap(DailymotionDownloader::getPlayerConfig)
                .map(jsonService::parse)
                .map(JsonService.to("metadata", DailymotionMetadata.class))
                .map(DailymotionMetadata::getUrl)
                .map(m3U8Service::getM3U8UrlFormMultiStreamFile)
                .map(this::removeHash);
    }

    private String removeHash(String s) {
        return StringUtils.substringBefore(s, "#");
    }

    private static Option<String> getPlayerConfig(String page) {
        if (!page.contains("var config = ") || !page.contains("}};")) {
            log.error("Structure of Dailymotion page changed");
            return None();
        }

        int begin = page.indexOf("var config = ");
        int end = page.indexOf("};", begin);
        return Option(page.substring(begin+"buildPlayer({".length()-1, end+1));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DailymotionMetadata {
        @JsonProperty("stream_chromecast_url") @Getter @Setter String url;
    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("dailymotion.com/video") ? 1 : Integer.MAX_VALUE;
    }

}
