package lan.dk.podcastserver.manager.worker.rss;

import com.github.davinkevin.podcastserver.service.ImageService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.manager.worker.Updater;
import com.github.davinkevin.podcastserver.service.JdomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import static io.vavr.API.*;
import static java.util.Objects.nonNull;

@Slf4j
@Component("RSSUpdater")
@RequiredArgsConstructor
public class RSSUpdater implements Updater {

    private static final Namespace MEDIA = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");
    private static final Namespace FEED_BURNER = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0");

    private final SignatureService signatureService;
    private final JdomService jdomService;
    private final ImageService imageService;


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
        return Option(element.getChild("thumbnail", MEDIA))
                .flatMap(t -> Option(t.getAttributeValue("url")).orElse(Option(t.getText())))
                .map(imageService::getCoverFromURL)
                .getOrElse(() -> null);
    }

    private ZonedDateTime getPubDate(Element item) {
        String date = Match(item.getChildText("pubDate")).of(
                Case($(s -> s.contains("EDT")), s -> s.replaceAll("EDT", "+0600")),
                Case($(s -> s.contains("PST")), s -> s.replaceAll("PST", "+0800")),
                Case($(s -> s.contains("PDT")), s -> s.replaceAll("PDT", "+0900")),
                Case($(), Function.identity())
        );

        return Try(() -> ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME))
                .onFailure(e -> log.error("Problem during date parsing of {}", item.getChildText("title"), e))
                .getOrNull();
    }

    @Override
    public String signatureOf(Podcast podcast) {
        return signatureService.fromUrl(podcast.getUrl());
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
