package lan.dk.podcastserver.manager.worker.finder;

import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.regex.Pattern;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static lan.dk.podcastserver.utils.MatcherExtractor.PatternExtractor;
import static lan.dk.podcastserver.utils.MatcherExtractor.from;

/**
 * Created by kevin on 20/07/2016.
 */
@Service
@RequiredArgsConstructor
public class TF1ReplayFinder implements Finder {

    private static final PatternExtractor PICTURE_EXTRACTOR = from(Pattern.compile("url\\(([^)]+)\\).*"));

    private final HtmlService htmlService;
    private final ImageService imageService;

    @Override
    public Podcast find(String url) {
        return htmlService.get(url)
                .map(htmlToPodcast(url))
                .getOrElse(Podcast.DEFAULT_PODCAST);
    }

    private Function<Document, Podcast> htmlToPodcast(String url) {
        return p -> Podcast.builder()
                        .title(p.select("meta[property=og:title]").attr("content"))
                        .description(p.select("meta[property=og:description]").attr("content"))
                        .url(url)
                        .cover(getCover(p))
                        .type("TF1Replay")
                    .build();
    }

    private Cover getCover(Document p) {
        return PICTURE_EXTRACTOR.on(p.select(".focalImg style").html()).group(1)
                .orElse(Option.of(p.select("meta[property=og:image]").attr("content")))
                .map(this::getUrl)
                .map(imageService::getCoverFromURL)
                .getOrElse(Cover.DEFAULT_COVER);
    }

    private String getUrl(String url) {
        return Match(url).of(
                Case($(u -> u.startsWith("//")), u -> "http:" + u),
                Case($(), Function.identity())
        );
    }


    @Override
    public Integer compatibility(@NotEmpty String url) {
        return StringUtils.contains(url, "www.tf1.fr") ? 1 : Integer.MAX_VALUE;
    }
}
