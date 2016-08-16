package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Component("RSSUpdater")
public class RSSUpdater extends AbstractUpdater {

    private static final Namespace MEDIA = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");
    private static final Namespace FEED_BURNER = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0");

    @Resource JdomService jdomService;
    @Resource ImageService imageService;

    public Set<Item> getItems(Podcast podcast) {
        log.debug("Traitement des Items");
        return jdomService
                .parse(podcast.getUrl())
                .map(p -> p.getRootElement().getChild("channel").getChildren("item"))
                .map(this::elementsToItems)
                .getOrElse(Sets.newHashSet());
    }

    private Set<Item> elementsToItems(List<Element> elements) {
        return elements
                .stream()
                    .filter(this::hasEnclosure)
                    .map(this::extractItem)
                .collect(toSet());
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
            log.error("Problem during date parsing", e);
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
