package lan.dk.podcastserver.manager.worker.tf1replay;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Finder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.PatternExtractor;
import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.from;
import static io.vavr.API.*;

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
                .orElse(Option(p.select("meta[property=og:image]").attr("content")))
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
