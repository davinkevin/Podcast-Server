package lan.dk.podcastserver.manager.worker.mycanal;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.TypeRef;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.manager.worker.Updater;
import lan.dk.podcastserver.service.JsonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import static io.vavr.API.Tuple;
import static lan.dk.podcastserver.manager.worker.mycanal.MyCanalModel.*;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class MyCanalUpdater implements Updater {

    private static final TypeRef<Set<MyCanalItem>> SET_OF_MY_CANAL_ITEM = new TypeRef<Set<MyCanalItem>>(){};
    private static final TypeRef<Set<MyCanalDetailsItem>> SET_OF_MY_CANAL_DETAILS_ITEM = new TypeRef<Set<MyCanalDetailsItem>>(){};
    private static final String URL_DETAILS = "https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/%s?format=json";
    private static final String DOMAIN = "https://www.mycanal.fr";

    private final SignatureService signatureService;
    private final JsonService jsonService;
    private final ImageService imageService;
    private final HtmlService htmlService;

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
                .map(signatureService::fromText)
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
                .flatMap(MyCanalUtils::extractJsonConfig)
                .map(jsonService::parse)
                .map(JsonService.to("landing.strates[*].contents[*]", SET_OF_MY_CANAL_ITEM))
                ;
    }

    private Option<MyCanalDetailsItem> findDetails(MyCanalItem item) {
        Try<DocumentContext> json = jsonService.parseUrl(String.format(URL_DETAILS, item.getContentID())).toTry();

        Supplier<Option<MyCanalDetailsItem>> fromCollection = () -> json
                .map(JsonService.to(SET_OF_MY_CANAL_DETAILS_ITEM))
                .getOrElse(HashSet::empty)
                .find(i -> i.getId().equalsIgnoreCase(item.getContentID()));

        return json
                .map(JsonService.to(MyCanalDetailsItem.class))
                .toOption()
                .orElse(fromCollection);
    }

    @Override
    public Type type() {
        return new Type("MyCanal", "MyCanal");
    }

    @Override
    public Integer compatibility(String url) {
        return MyCanalUtils.compatibility(url);
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
}
