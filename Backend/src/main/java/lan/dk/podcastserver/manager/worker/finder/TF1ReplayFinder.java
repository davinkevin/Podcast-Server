package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 20/07/2016.
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TF1ReplayFinder implements Finder {

    public static final Pattern PICTURE_EXTRACTOR = Pattern.compile("url\\(([^)]+)\\).*");
    final HtmlService htmlService;
    final ImageService imageService;

    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {
        return htmlService
                .get(url)
                .map(htmlToPodcast(url))
                .orElse(Podcast.DEFAULT_PODCAST);
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

        Matcher m = PICTURE_EXTRACTOR.matcher(style);

        if (!m.find())
            return imageService.getCoverFromURL(p.select("meta[property=og:image]").attr("content"));

        return imageService.getCoverFromURL(m.group(1));
    }


    @Override
    public Integer compatibility(@NotEmpty String url) {
        return StringUtils.contains(url, "www.tf1.fr") ? 1 : Integer.MAX_VALUE;
    }
}
