package lan.dk.podcastserver.manager.worker.finder;

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

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
@Service("CanalPlusFinder")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CanalPlusFinder implements Finder {

    final HtmlService htmlService;
    final ImageService imageService;

    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {
        return htmlService
                .get(url)
                .map(this::htmlToPodcast)
                .orElse(Podcast.DEFAULT_PODCAST);
    }

    private Podcast htmlToPodcast(Document p) {
        return Podcast.builder()
                    .title(p.select("meta[name=twitter:title]").attr("content"))
                    .description(p.select("meta[name=description]").attr("content"))
                    .cover(imageService.getCoverFromURL(p.select("meta[property=og:image]").attr("content")))
                    .url(p.select("meta[property=og:url]").attr("content"))
                    .type("CanalPlus")
                .build();
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return StringUtils.contains(url, "canalplus.fr") ? 1 : Integer.MAX_VALUE;
    }
}
