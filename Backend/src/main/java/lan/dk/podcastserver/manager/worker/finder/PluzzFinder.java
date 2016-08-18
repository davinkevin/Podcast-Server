package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
@Service("PluzzFinder")
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class PluzzFinder implements Finder {

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
                    .title(p.select("meta[name=programme_titre]").attr("content"))
                    .description(p.select("meta[name=description]").attr("content"))
                    .cover(imageService.getCoverFromURL(p.select("meta[name=programme_image]").attr("content")))
                    .url(p.select("meta[name=og:url]").attr("content"))
                    .type("Pluzz")
                .build();
    }



    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("pluzz.francetv.fr") ? 1 : Integer.MAX_VALUE;
    }
}
