package lan.dk.podcastserver.manager.worker.updater;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jayway.jsonpath.TypeRef;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.finder.MyCanalFinder;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static io.vavr.API.Option;
import static io.vavr.API.Tuple;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
public class MyCanalUpdater extends AbstractUpdater {

    private static final String MYCANAL_DATE_PATTERN = "dd/MM/yyyy-HH:mm:ss";
    private static final TypeRef<Set<MyCanalItem>> SET_OF_MY_CANAL_ITEM = new TypeRef<Set<MyCanalItem>>(){};
    private static final String URL_DETAILS = "https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/%s?format=json";
    private static final String DOMAIN = "https://www.mycanal.fr";

    private final JsonService jsonService;
    private final ImageService imageService;
    private final HtmlService htmlService;

    protected MyCanalUpdater(PodcastServerParameters podcastServerParameters, SignatureService signatureService, Validator validator, JsonService jsonService, ImageService imageService, HtmlService htmlService) {
        super(podcastServerParameters, signatureService, validator);
        this.jsonService = jsonService;
        this.imageService = imageService;
        this.htmlService = htmlService;
    }

    @Override
    public Set<Item> getItems(Podcast p) {
        return itemsAsJsonFrom(p)
                .getOrElse(HashSet::empty).toMap(v -> Tuple(v, this.findDetails(v)))
                .filterValues(Option::isDefined)
                .mapValues(Option::get)
                .mapValues(this::asItem)
                .toSet()
                .map(v -> v._2().url(DOMAIN + v._1().getOnClick().getPath()).build());
    }

    @Override
    public String signatureOf(Podcast p) {
        return itemsAsJsonFrom(p)
                .map(items -> items.toList().map(MyCanalItem::getContentID).sorted().mkString())
                .map(signatureService::generateMD5Signature)
                .getOrElseThrow(() -> new RuntimeException("Error during signature of " + p.getTitle() + " with url " + p.getUrl()));
    }

    private Option<Set<MyCanalItem>> itemsAsJsonFrom(Podcast p) {
        return htmlService
                .get(p.getUrl())
                .map(Document::body)
                .map(es -> List.ofAll(es.select("script")))
                .getOrElse(List::empty)
                .find(e -> e.html().contains("__data"))
                .map(Element::html)
                .flatMap(MyCanalFinder::extractJsonConfig)
                .map(jsonService::parse)
                .map(JsonService.to("landing.strates[0].contents", SET_OF_MY_CANAL_ITEM))
                ;
    }

    private Option<MyCanalDetailsItem> findDetails(MyCanalItem item) {
        return jsonService.parseUrl(String.format(URL_DETAILS, item.getContentID()))
                .map(JsonService.to(MyCanalDetailsItem.class))
                ;
    }

    @Override
    public Type type() {
        return new Type("MyCanal", "MyCanal");
    }

    @Override
    public Integer compatibility(String url) {
        return MyCanalFinder._compatibility(url);
    }

    private Item.ItemBuilder asItem(MyCanalDetailsItem i) {
        MyCanalInfosItem infos = i.getInfos();
        MyCanalTitrageItem titrage = infos.getTitrage();
        MyCanalPublicationItem publication = infos.getPublication();

        return Item.builder()
                .title(titrage.getTitre())
                .description(titrage.getSous_titre())
                .length(i.getDuration())
                .cover(i.getMedia().getImages().cover().map(imageService::getCoverFromURL).getOrElse(Cover.DEFAULT_COVER))
                .pubDate(publication.asZonedDateTime())
                ;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyCanalItem {
        @Getter @Setter String contentID;
        @Getter @Setter MyCanalItemOnClick onClick;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyCanalItemOnClick {
        @Getter @Setter String path;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyCanalDetailsItem {
        @JsonProperty("ID") @Getter @Setter String id;
        @JsonProperty("DURATION") @Getter @Setter Long duration;
        @JsonProperty("INFOS") @Getter @Setter MyCanalInfosItem infos;
        @JsonProperty("MEDIA") @Getter @Setter MyCanalMediaItem media;
        @JsonProperty("URL") @Getter @Setter String url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyCanalInfosItem {
        @JsonProperty("DESCRIPTION") @Getter @Setter String description;
        @JsonProperty("PUBLICATION") @Getter @Setter MyCanalPublicationItem publication;
        @JsonProperty("TITRAGE") @Getter @Setter MyCanalTitrageItem titrage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyCanalPublicationItem {
        @JsonProperty("DATE") @Getter @Setter String date;
        @JsonProperty("HEURE") @Getter @Setter String heure;

        ZonedDateTime asZonedDateTime() {
            LocalDateTime localDateTime = LocalDateTime.parse(date.concat("-").concat(heure), DateTimeFormatter.ofPattern(MYCANAL_DATE_PATTERN));
            return ZonedDateTime.of(localDateTime, ZoneId.of("Europe/Paris"));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyCanalTitrageItem {
        @JsonProperty("TITRE") @Getter @Setter String titre;
        @JsonProperty("SOUS_TITRE") @Getter @Setter String sous_titre;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyCanalMediaItem {
        @JsonProperty("IMAGES") @Getter @Setter MyCanalImageItem images;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MyCanalImageItem {
        @JsonProperty("GRAND") @Getter @Setter String grand;
        @JsonProperty("PETIT") @Getter @Setter String petit;

        Option<String> cover() {
            return Option(grand).orElse(Option(petit));
        }
    }


}
