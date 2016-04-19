package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.HtmlService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Function;

/**
 * Created by kevin on 22/03/2016 for Podcast Server
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JeuxVideoComFinder implements Finder {

    final HtmlService htmlService;

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
                    .description(p.select("meta[name=description]").attr("content"))
                    .url(url)
                    .type("JeuxVideoCom")
                .build();
    }

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "jeuxvideo.com") ? 1 : Integer.MAX_VALUE;
    }
}
