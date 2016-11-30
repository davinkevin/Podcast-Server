package lan.dk.podcastserver.manager.worker.updater;

import javaslang.collection.HashSet;
import javaslang.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.SignatureService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by kevin on 18/12/14.
 */
@Slf4j
@Component("JeuxVideoComUpdater")
public class JeuxVideoComUpdater extends AbstractUpdater {

    static final String JEUXVIDEOCOM_HOST = "http://www.jeuxvideo.com";
    private final HtmlService htmlService;
    private final ImageService imageService;

    public JeuxVideoComUpdater(PodcastServerParameters podcastServerParameters, SignatureService signatureService, Validator validator, HtmlService htmlService, ImageService imageService) {
        super(podcastServerParameters, signatureService, validator);
        this.htmlService = htmlService;
        this.imageService = imageService;
    }


    @Override
    public Set<Item> getItems(Podcast podcast) {
        return htmlService.get(podcast.getUrl())
                .map(p -> p.select("article"))
                .map(this::htmlToItems)
                .getOrElse(HashSet::empty);
    }

    private Set<Item> htmlToItems(Elements elements) {
        return HashSet.ofAll(elements)
                .map(element -> generateItemFromPage(element.select("a").first().attr("href")));
    }

    private Item generateItemFromPage(String videoPageUrl) {
        return htmlService
                .get(JEUXVIDEOCOM_HOST.concat(videoPageUrl))
                .map(this::htmlToItem)
                .getOrElse(Item.DEFAULT_ITEM);
    }

    private Item htmlToItem(Document page) {
        Elements headerVideo = page.select(".header-video");
        return Item.builder()
                .title(headerVideo.select("meta[itemprop=name]").attr("content"))
                .description(page.select(".corps-video p").text())
                .url(headerVideo.select("meta[itemprop=contentUrl]").attr("content"))
                .pubDate(ZonedDateTime.of(LocalDateTime.parse(headerVideo.select(".date-comm time").attr("datetime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME), ZoneId.of("Europe/Paris")))
                .cover(imageService.getCoverFromURL(headerVideo.select("meta[itemprop=thumbnailUrl]").attr("content")))
            .build();
    }

    @Override
    public String signatureOf(Podcast podcast) {
        return htmlService
                .get(podcast.getUrl())
                .map(p -> p.select("article").html())
                .map(signatureService::generateMD5Signature)
                .getOrElse("");
    }

    @Override
    public Type type() {
        return new Type("JeuxVideoCom", "JeuxVideo.com");
    }

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "jeuxvideo.com")
                ? 1
                : Integer.MAX_VALUE;
    }
}
