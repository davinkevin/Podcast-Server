package lan.dk.podcastserver.manager.worker.mycanal;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import com.jayway.jsonpath.DocumentContext;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Finder;
import lan.dk.podcastserver.service.JsonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;

import static lan.dk.podcastserver.manager.worker.mycanal.MyCanalModel.MyCanalItem;
import static lan.dk.podcastserver.manager.worker.mycanal.MyCanalModel.MyCanalPageItem;

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
@Slf4j
@Service("MyCanalFinder")
@RequiredArgsConstructor
public class MyCanalFinder implements Finder {

    private static final String DOMAIN = "https://www.mycanal.fr";
    private final HtmlService htmlService;
    private final ImageService imageService;
    private final JsonService jsonService;

    @Override
    public Podcast find(String url) {
        Option<DocumentContext> json = htmlService
                .get(url)
                .map(Document::body)
                .flatMap(es -> List.ofAll(es.select("script")).find(e -> e.html().contains("app_config")))
                .map(Element::html)
                .flatMap(MyCanalUtils::extractJsonConfig)
                .map(jsonService::parse);

        return json.toTry().map(JsonService.to("landing.cover", MyCanalItem.class)).map(this::asPodcast)
                .orElse(() -> json.toTry().map(JsonService.to("page", MyCanalPageItem.class)).map(this::asPodcast))
                .getOrElse(Podcast.DEFAULT_PODCAST);
    }

     private Podcast asPodcast(MyCanalItem item) {
        return Podcast.builder()
                .title(item.getOnClick().getDisplayName())
                .url(DOMAIN + item.getOnClick().getPath())
                .cover(imageService.getCoverFromURL(item.getImage()))
                .type("MyCanal")
                .build();
    }

    private Podcast asPodcast(MyCanalPageItem item) {
        return Podcast.builder()
                .title(item.getDisplayName())
                .url(DOMAIN + item.getPathname())
                .cover(Cover.DEFAULT_COVER)
                .type("MyCanal")
                .build();
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return MyCanalUtils.compatibility(url);
    }

}
