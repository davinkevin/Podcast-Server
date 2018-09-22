package lan.dk.podcastserver.manager.worker.gulli;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Finder;
import lombok.AllArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;

import static io.vavr.API.Option;

/**
 * Created by kevin on 04/10/2016 for Podcast Server
 */
@Service("GulliFinder")
@AllArgsConstructor
public class GulliFinder implements Finder {

    private static final String COVER_SELECTOR = "div.program_gullireplay a[href=%s] img";

    private final HtmlService htmlService;
    private final ImageService imageService;

    @Override
    public Podcast find(String url) {
        return htmlService.get(url)
                .map(this::htmlToPodcast)
                .getOrElse(Podcast.DEFAULT_PODCAST);
    }

    private Podcast htmlToPodcast(Document d) {
        return Podcast.builder()
                    .title(d.select("ol.breadcrumb li.active").first().text())
                    .cover(coverOf(d))
                    .description(d.select("meta[property=og:description]").attr("content"))
                    .url(d.select("meta[property=og:url]").attr("content"))
                    .type("Gulli")
                .build();
    }

    private Cover coverOf(Document d) {
        String pageUrl = d.select("meta[property=og:url]").attr("content");

        return Option(d.select(String.format(COVER_SELECTOR, pageUrl)).first())
                        .map(e -> e.attr("src"))
                        .map(imageService::getCoverFromURL)
                        .getOrElse(Cover.DEFAULT_COVER);
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("replay.gulli.fr") ? 1 : Integer.MAX_VALUE;
    }
}
