package lan.dk.podcastserver.manager.worker.updater;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.springframework.stereotype.Component;

import javax.validation.Validator;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Component("RSSUpdater")
public class RSSUpdater extends AbstractUpdater {

    private static final Namespace MEDIA = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");
    private static final Namespace FEED_BURNER = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0");

    private final JdomService jdomService;
    private final ImageService imageService;

    public RSSUpdater(PodcastServerParameters podcastServerParameters, SignatureService signatureService, Validator validator, JdomService jdomService, ImageService imageService) {
        super(podcastServerParameters, signatureService, validator);
        this.jdomService = jdomService;
        this.imageService = imageService;
    }


    public Set<Item> getItems(Podcast podcast) {
        return jdomService
                .parse(podcast.getUrl())
                .map(p -> p.getRootElement().getChild("channel").getChildren("item"))
                .map(HashSet::ofAll)
                .map(this::elementsToItems)
                .getOrElse(HashSet::empty);
    }

    private Set<Item> elementsToItems(Set<Element> elements) {
        return HashSet.ofAll(elements)
                    .filter(this::hasEnclosure)
                    .map(this::extractItem);
    }

    private Boolean hasEnclosure(Element item) {
        return item.getChild("enclosure") != null || item.getChild("origEnclosureLink", FEED_BURNER) != null;
    }

    private Item extractItem(Element item) {
        // Gestion des cas pour l'url :
        return Item.builder()
                    .title(item.getChildText("title"))
                    .pubDate(getPubDate(item))
                    .description(item.getChildText("description"))
                    .mimeType(item.getChild("enclosure").getAttributeValue("type"))
                    .length(lengthOf(item))
                    .cover(coverOf(item))
                    .url(urlOf(item))
                .build();
    }

    private long lengthOf(Element item) {
        return (StringUtils.isNotEmpty(item.getChild("enclosure").getAttributeValue("length")))
                ? Long.parseLong(item.getChild("enclosure").getAttributeValue("length"))
                : 0L;
    }

    private String urlOf(Element element) {
        if (nonNull(element.getChild("origEnclosureLink", FEED_BURNER))) {
            return element.getChildText("origEnclosureLink", FEED_BURNER);
        }

        return element.getChild("enclosure").getAttributeValue("url");
    }

    private Cover coverOf(Element element) {
        Element thumbnail = element.getChild("thumbnail", MEDIA);
        if (isNull(thumbnail)) {
            return null;
        }

        if (nonNull(thumbnail.getAttributeValue("url"))) {
            return imageService.getCoverFromURL(thumbnail.getAttributeValue("url"));
        }

        return imageService.getCoverFromURL(thumbnail.getText());
    }

    private ZonedDateTime getPubDate(Element item) {
        try {
            return ZonedDateTime.parse(item.getChildText("pubDate").replace(" PST", " +0800"), DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (Exception e) {
            log.error("Problem during date parsing of {}", item.getChildText("title"), e);
            // No better idea than returning null for unparseable date
            return null;
        }
    }

    @Override
    public String signatureOf(Podcast podcast) {
        return signatureService.generateSignatureFromURL(podcast.getUrl());
    }

    @Override
    public Type type() {
        return new Type("RSS", "RSS");
    }

    @Override
    public Integer compatibility(String url) {
        return Integer.MAX_VALUE-1;
    }
}
