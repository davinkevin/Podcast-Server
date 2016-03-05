package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.jdom2.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 22/02/15.
 */
@Service("RSSFinder")
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class RSSFinder implements Finder {

    public static final String CHANNEL = "channel";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String IMAGE = "image";
    public static final String URL = "url";
    public static final String HREF = "href";

    final UrlService urlService;
    final JdomService jdomService;
    final ImageService imageService;

    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {
        return urlService
                .newURL(url)
                .flatMap(jdomService::parse)
                .map(x -> x.getRootElement().getChild(CHANNEL))
                .filter(Objects::nonNull)
                .map(e -> this.xmlToPodcast(e, url))
                .orElse(Podcast.DEFAULT_PODCAST);
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

}
