package lan.dk.podcastserver.manager.worker.francetv;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Finder;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.UrlService;
import lombok.RequiredArgsConstructor;
import javax.validation.constraints.NotEmpty;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import static io.vavr.API.Option;

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
@Service("FranceTvFinder")
@RequiredArgsConstructor
public class FranceTvFinder implements Finder {

    private final HtmlService htmlService;
    private final ImageService imageService;

    @Override
    public Podcast find(String url) {
        return htmlService.get(url)
                .map(this::htmlToPodcast)
                .getOrElse(Podcast.DEFAULT_PODCAST);
    }

    private Podcast htmlToPodcast(Document p) {
        return Podcast.builder()
                    .title(p.select("meta[property=og:title]").attr("content"))
                    .description(p.select("meta[property=og:description]").attr("content"))
                    .cover(getCover(p))
                    .url(UrlService.addProtocolIfNecessary("https:", p.select("meta[property=og:url]").attr("content")))
                    .type("FranceTv")
                .build();
    }

    private Cover getCover(Document p) {
        return Option(p.select("meta[property=og:image]"))
            .map(e -> e.attr("content"))
            .map(s -> "https:" + s)
            .map(imageService::getCoverFromURL)
            .getOrElse(Cover.DEFAULT_COVER);
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return FranceTvUpdater.isFromFranceTv(url);
    }
}
