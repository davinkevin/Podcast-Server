package lan.dk.podcastserver.manager.worker.finder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jayway.jsonpath.DocumentContext;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import static io.vavr.API.*;

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
@Slf4j
@Service("MyCanalFinder")
@RequiredArgsConstructor
public class MyCanalFinder implements Finder {

    private static final String DOMAIN = "https://www.mycanal.fr";
    private final HtmlService htmlService;
    private final ImageService imageService;
    private final JsonService jsonService;

    @Override
    public Podcast find(String url) {
        Option<DocumentContext> json = htmlService
                .get(url)
                .map(Document::body)
                .flatMap(es -> List.ofAll(es.select("script")).find(e -> e.html().contains("app_config")))
                .map(Element::html)
                .flatMap(MyCanalFinder::extractJsonConfig)
                .map(jsonService::parse);

        return json.toTry().map(JsonService.to("landing.cover", MyCanalLandingItem.class)).map(this::asPodcast)
                .orElse(() -> json.toTry().map(JsonService.to("page", MyCanalPageItem.class)).map(this::asPodcast))
                .getOrElse(Podcast.DEFAULT_PODCAST);
    }

     private Podcast asPodcast(MyCanalLandingItem item) {
        return Podcast.builder()
                .title(item.getOnClick().getDisplayName())
                .url(DOMAIN + item.getOnClick().getPath())
                .cover(imageService.getCoverFromURL(item.getImage()))
                .type("MyCanal")
                .build();
    }

    private Podcast asPodcast(MyCanalPageItem item) {
        return Podcast.builder()
                .title(item.getDisplayName())
                .url(DOMAIN + item.getPathname())
                .cover(Cover.DEFAULT_COVER)
                .type("MyCanal")
                .build();
    }

    public static Option<String> extractJsonConfig(String text) {
        String startToken = "__data=";
        String endToken = "};";

        if (!text.contains(startToken) || !text.contains(endToken)) {
            log.error("Structure of MyCanal page changed");
            return None();
        }

        int begin = text.indexOf(startToken);
        int end = text.indexOf(endToken, begin);
        return Option(text.substring(begin + startToken.length(), end + 1));
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return _compatibility(url);
    }

    public static int _compatibility(@NotEmpty String url) {
        return StringUtils.contains(url, "www.mycanal.fr") ? 1 : Integer.MAX_VALUE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyCanalLandingItem {
        @Getter @Setter String image;
        @Getter @Setter MyCanalOnClick onClick;

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class MyCanalOnClick {
            @Getter @Setter String displayName;
            @Getter @Setter String path;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyCanalPageItem {
        @Getter @Setter String displayName;
        @Getter @Setter String pathname;
    }



}
