package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by kevin on 18/03/2016 for Podcast Server
 */
@Service
@RequiredArgsConstructor
public class BeInSportsFinder implements Finder{

    final HtmlService htmlService;
    final ImageService imageService;

    @Override
    public Podcast find(String url) {
        return htmlService
                .get(url)
                .map(this::htmlToPodcast)
                .getOrElse(Podcast.DEFAULT_PODCAST);
    }

    private Podcast htmlToPodcast(Document p) {
        return Podcast.builder()
                    .title(p.select("select#replay__shows option[selected]").text())
                    .cover(imageService.getCoverFromURL(p.select("meta[property=og:image]").attr("content")))
                    .url(p.select("meta[property=og:url]").attr("content"))
                    .type("BeInSports")
                .build();
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return StringUtils.contains(url, "beinsports.com") ? 1 : Integer.MAX_VALUE;
    }
}
