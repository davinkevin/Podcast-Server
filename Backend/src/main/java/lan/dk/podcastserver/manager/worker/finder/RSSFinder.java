package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.jdom2.Element;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 22/02/15.
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

    final JdomService jdomService;
    final ImageService imageService;

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
                .cover(imageService.getCoverFromURL(coverUrlOf(element)))
            .build();
    }

    private String coverUrlOf(Element channelElement) {
        Element rssImage = channelElement.getChild(IMAGE);
        if (nonNull(rssImage)) {
            return rssImage.getChildText(URL);
        }

        Element itunesImage = channelElement.getChild(IMAGE, JdomService.ITUNES_NAMESPACE);
        if (nonNull(itunesImage))
            return itunesImage.getAttributeValue(HREF);

        return null;
    }

    public Integer compatibility(@NotEmpty String url) {
        return Integer.MAX_VALUE-1;
    }

}
