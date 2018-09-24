package lan.dk.podcastserver.manager.worker.rss;

import com.github.davinkevin.podcastserver.service.ImageService;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Finder;
import com.github.davinkevin.podcastserver.service.JdomService;
import lombok.RequiredArgsConstructor;
import org.jdom2.Element;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;
import java.util.Objects;

import static io.vavr.API.Option;

/**
 * Created by kevin on 22/02/15
 */
@Service("RSSFinder")
@RequiredArgsConstructor
public class RSSFinder implements Finder {

    private static final String CHANNEL = "channel";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String IMAGE = "image";
    private static final String URL = "url";
    private static final String HREF = "href";

    private final JdomService jdomService;
    private final ImageService imageService;

    @Override
    public Podcast find(String url) {
        return jdomService
                .parse(url)
                .map(x -> x.getRootElement().getChild(CHANNEL))
                .filter(Objects::nonNull)
                .map(e -> this.xmlToPodcast(e, url))
                .getOrElse(Podcast.DEFAULT_PODCAST);
    }

    private Podcast xmlToPodcast(Element element, String url) {
        return Podcast.builder()
                .type("RSS")
                .url(url)
                .title(element.getChildText(TITLE))
                .description(element.getChildText(DESCRIPTION))
                .cover(coverUrlOf(element))
                .build();
    }

    private Cover coverUrlOf(Element channelElement) {
        return getRssImage(channelElement)
                .orElse(getItunesImage(channelElement))
                .map(imageService::getCoverFromURL)
                .getOrElse(Cover.DEFAULT_COVER);
    }

    private Option<String> getItunesImage(Element channelElement) {
        return Option(channelElement.getChild(IMAGE, JdomService.Companion.getITUNES_NAMESPACE()))
                .map(itunesImage -> itunesImage.getAttributeValue(HREF));
    }

    private Option<String> getRssImage(Element channelElement) {
        return Option(channelElement.getChild(IMAGE))
                .map(rssImage -> rssImage.getChildText(URL));
    }

    public Integer compatibility(@NotEmpty String url) {
        return Integer.MAX_VALUE-1;
    }

}
