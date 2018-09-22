package lan.dk.podcastserver.manager.worker.mycanal;

import com.github.davinkevin.podcastserver.service.HtmlService;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.manager.worker.Extractor;
import lan.dk.podcastserver.service.JsonService;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static io.vavr.API.List;
import static lan.dk.podcastserver.manager.worker.mycanal.MyCanalModel.MyCanalVideoItem;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Created by kevin on 24/12/2017
 */
@Component
@Scope(SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class MyCanalExtractor implements Extractor {

    private static final String URL_DETAILS = "https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/%s?format=json";

    private final HtmlService htmlService;
    private final JsonService jsonService;

    @Override
    public DownloadingItem extract(Item item) {
        return htmlService.get(item.getUrl())
                .map(Document::body)
                .map(es -> List.ofAll(es.select("script")))
                .getOrElse(List::empty)
                .find(e -> e.html().contains("__data"))
                .map(Element::html)
                .flatMap(MyCanalUtils::extractJsonConfig)
                .map(jsonService::parse)
                .map(JsonService.to("detailPage.body.contentID", String.class))
                .flatMap(this::findDetails)
                .map(v -> DownloadingItem.builder()
                        .filename(getFileName(item))
                        .urls(List(v.getHls()))
                        .item(item)
                        .build())
                .getOrElseThrow(() -> new RuntimeException("Error during extraction of " + item.getTitle() + " at url " + item.getUrl()))
        ;
    }

    @Override
    public String getFileName(Item item) {
        return Extractor.super.getFileName(item) + ".mp4";
    }

    @Override
    public Integer compatibility(String url) {
        return MyCanalUtils.compatibility(url);
    }

    private Option<MyCanalVideoItem> findDetails(String id) {
        return jsonService.parseUrl(String.format(URL_DETAILS, id))
                .map(JsonService.to("MEDIA.VIDEOS", MyCanalVideoItem.class));
    }
}
