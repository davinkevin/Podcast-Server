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

import static io.vavr.API.*;

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
@Service("CanalPlusFinder")
@RequiredArgsConstructor
public class CanalPlusFinder implements Finder {

    private final HtmlService htmlService;
    private final ImageService imageService;

    @Override
    public Podcast find(String url) {
        return htmlService
                .get(url)
                .map(this::htmlToPodcast)
                .getOrElse(Podcast.DEFAULT_PODCAST);
    }

    private Podcast htmlToPodcast(Document p) {
        return Podcast.builder()
                    .title(p.select("meta[name=twitter:title]").attr("content"))
                    .description(p.select("meta[name=description]").attr("content"))
                    .cover(getCover(p))
                    .url(p.select("meta[property=og:url]").attr("content"))
                    .type("CanalPlus")
                .build();
    }

    private Cover getCover(Document p) {
        return this.getCoverFromMetaOgUrl(p)
                .orElse(() -> this.getCoverFromBanner(p))
                .map(imageService::getCoverFromURL)
                .getOrElse(Cover.DEFAULT_COVER);
    }

    private Option<String> getCoverFromBanner(Document p) {
        return Option(p.select(".titreTxtBrut img").first())
                .map(e -> e.attr("src"));
    }

    private Option<String> getCoverFromMetaOgUrl(Document p) {
        return Option(p.select("meta[property=og:image]").first())
                .map(e -> e.attr("content"));
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return StringUtils.contains(url, "canalplus.fr") ? 1 : Integer.MAX_VALUE;
    }
}
