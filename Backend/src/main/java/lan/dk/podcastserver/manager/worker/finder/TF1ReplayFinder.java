package lan.dk.podcastserver.manager.worker.finder;

import javaslang.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.utils.MatcherExtractor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Created by kevin on 20/07/2016.
 */
@Service
@RequiredArgsConstructor
public class TF1ReplayFinder implements Finder {

    private static final Pattern PICTURE_EXTRACTOR = Pattern.compile("url\\(([^)]+)\\).*");

    private final HtmlService htmlService;
    private final ImageService imageService;

    @Override
    public Podcast find(String url) {
        return htmlService
                .get(url)
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
        String style = p.select(".focalImg style").html();

        return MatcherExtractor.of(PICTURE_EXTRACTOR, style).group(1)
                .orElse(Option.of(p.select("meta[property=og:image]").attr("content")))
                .map(url -> url.startsWith("//") ? "http:" + url : url)
                .map(imageService::getCoverFromURL)
                .getOrElse(Cover.DEFAULT_COVER);
    }


    @Override
    public Integer compatibility(@NotEmpty String url) {
        return StringUtils.contains(url, "www.tf1.fr") ? 1 : Integer.MAX_VALUE;
    }
}
