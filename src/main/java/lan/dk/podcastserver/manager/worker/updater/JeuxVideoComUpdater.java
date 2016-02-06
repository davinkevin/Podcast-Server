package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Created by kevin on 18/12/14.
 */
@Slf4j
@Component("JeuxVideoComUpdater")
public class JeuxVideoComUpdater extends AbstractUpdater {

    public static final String JEUXVIDEOCOM_HOST = "http://www.jeuxvideo.com";
    @Resource HtmlService htmlService;
    @Resource ImageService imageService;

    @Override
    public Set<Item> getItems(Podcast podcast) {
        Document page;

        try {
            page = htmlService.connectWithDefault(podcast.getUrl()).get();
        } catch (IOException e) {
            log.error("IOException :", e);
            return Sets.newHashSet();
        }

        return page.select("article")
                .stream()
                .map(element -> generateItemFromPage(element.select("a").first().attr("href")))
                .collect(toSet());
    }

    private Item generateItemFromPage(String videoPageUrl) {
        String completeUrl = JEUXVIDEOCOM_HOST.concat(videoPageUrl);
        Document page;

        try {
            page = htmlService.connectWithDefault(completeUrl).get();
        } catch (IOException e) {
            log.error("IOException :", e);
            return Item.DEFAULT_ITEM;
        }

        Elements selectedArea = page.select(".header-video");

        return new Item()
                .setTitle(selectedArea.select("meta[itemprop=name]").attr("content"))
                .setDescription(page.select(".corps-video p").text())
                .setUrl(selectedArea.select("meta[itemprop=contentUrl]").attr("content"))
                .setPubdate(ZonedDateTime.of(LocalDateTime.parse(selectedArea.select(".date-comm time").attr("datetime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME), ZoneId.of("Europe/Paris")))
                .setCover(imageService.getCoverFromURL(selectedArea.select("meta[itemprop=thumbnailUrl]").attr("content")));
    }

    @Override
    public String signatureOf(Podcast podcast) {
        try {
            Document page = htmlService.connectWithDefault(podcast.getUrl()).get();
            return signatureService.generateMD5Signature(page.select("article").html());
        } catch (IOException e) {
            log.error("IOException :", e);
        }

        return "";
    }

    @Override
    public Type type() {
        return new Type("JeuxVideoCom", "JeuxVideo.com");
    }
}
