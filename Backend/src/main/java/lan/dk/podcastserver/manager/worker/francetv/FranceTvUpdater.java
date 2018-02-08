package lan.dk.podcastserver.manager.worker.francetv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.manager.worker.Updater;
import lan.dk.podcastserver.service.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.util.Objects.isNull;

/**
 * Created by kevin on 30/06/2017.
 */
@Slf4j
@Component("FranceTvUpdater")
@RequiredArgsConstructor
public class FranceTvUpdater implements Updater {
    private static final String LAST_VIDEOS_SELECTOR = "ul.wall";
    private static final String CATALOG_URL = "https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s";

    private final SignatureService signatureService;
    private final HtmlService htmlService;
    private final ImageService imageService;
    private final JsonService jsonService;

    public Set<Item> getItems(Podcast podcast) {
        return htmlService.get(podcast.getUrl())
                .map(p -> p.select(LAST_VIDEOS_SELECTOR))
                .flatMap(v -> HashSet.ofAll(v).find(ul -> "wall".equals(ul.className())))
                .map(ul -> ul.select("li"))
                .map(HashSet::ofAll).getOrElse(HashSet::empty)
                .map(this::htmlToItem);
    }

    private Item htmlToItem(Element element) {
        return HashSet.ofAll(element.children())
                .find(e -> Objects.equals(e.tagName(), "a"))
                .map(e -> e.attr("data-video"))
                .map(id -> String.format(CATALOG_URL, id))
                .flatMap(jsonService::parseUrl)
                .map(JsonService.to(FranceTvItem.class))
                .map(ftv -> Item.builder()
                        .title(ftv.title())
                        .description(ftv.getSynopsis())
                        .pubDate(ftv.pubDate())
                        .url(getUrl(element))
                        .cover(imageService.getCoverFromURL(ftv.getImage()))
                        .build()
                )
                .getOrElse(Item.DEFAULT_ITEM);
    }

    private String getUrl(Element element) {
        return HashSet.ofAll(element.children())
                .find(e -> Objects.equals(e.tagName(), "a"))
                .map(e -> UrlService.addProtocolIfNecessary("https:", e.attr("href")))
                .getOrElse("");
    }

    @Override
    public String signatureOf(Podcast podcast) {
        String listOfIds = htmlService.get(podcast.getUrl())
                .map(p -> p.select(LAST_VIDEOS_SELECTOR))
                .flatMap(v -> HashSet.ofAll(v).find(ul -> "wall".equals(ul.className())))
                .map(ul -> ul.select("li"))
                .map(HashSet::ofAll).getOrElse(HashSet::empty)
                .flatMap(li -> HashSet.ofAll(li.children()).find(el -> Objects.equals(el.tagName(), "a")))
                .map(e -> e.attr("data-video"))
                .toList().sorted().mkString("-");

        return signatureService.generateMD5Signature(listOfIds);
    }

    @Override
    public Type type() {
        return new Type("FranceTv", "France•tv");
    }

    @Override
    public Integer compatibility(String url) {
        return isFromFranceTv(url);
    }

    public static int isFromFranceTv(String url) {
        return StringUtils.contains(url, "www.france.tv") ? 1 : Integer.MAX_VALUE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FranceTvItem {

        private static final ZoneId ZONE_ID = ZoneId.of("Europe/Paris");

        @Setter private String titre;
        @JsonProperty("sous_titre") @Setter private String sousTitre;
        @Setter @Getter private String synopsis;

        @Setter private String saison;
        @Setter private String episode;
        @Setter private FranceTvItem.Diffusion diffusion = new FranceTvItem.Diffusion();
        @JsonProperty("image_secure") @Getter @Setter private String image;
        @Setter @Getter private List<FranceTvItem.Video> videos = List.empty();

        String title() {
            String title = titre;

            if (StringUtils.isNotEmpty(saison)) {
                title = title + " - S" + saison;
            }

            if (StringUtils.isNotEmpty(episode)) {
                title = title + "E" + episode;
            }

            if (StringUtils.isNotEmpty(sousTitre)) {
                title = title + " - " + sousTitre;
            }

            return title;
        }

        ZonedDateTime pubDate() {
            if (isNull(diffusion.getTimestamp())) {
                return null;
            }

            return ZonedDateTime.ofInstant(Instant.ofEpochSecond(diffusion.getTimestamp()), ZONE_ID);
        }


        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class Diffusion {
            @Setter @Getter private Long timestamp;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class Video {
            @Setter  private String format;
            @Setter @Getter private String url;
        }
    }
}
