package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by kevin on 18/12/14.
 */
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
            logger.error("IOException :", e);
            return new HashSet<>();
        }

        return page.select("article")
                .stream()
                .map(element -> generateItemFromPage(element.select("a").first().attr("href")))
                .collect(Collectors.toSet());
    }

    private Item generateItemFromPage(String videoPageUrl) {
        String completeUrl = JEUXVIDEOCOM_HOST.concat(videoPageUrl);
        Document page;

        try {
            page = htmlService.connectWithDefault(completeUrl).get();
        } catch (IOException e) {
            logger.error("IOException :", e);
            return new Item();
        }

        Elements selectedArea = page.select(".header-video");

        Item item = new Item()
                .setTitle(selectedArea.select("meta[itemprop=name]").attr("content"))
                .setDescription(page.select(".corps-video p").text())
                .setUrl(selectedArea.select("meta[itemprop=contentUrl]").attr("content"))
                .setPubdate(ZonedDateTime.of(LocalDateTime.parse(selectedArea.select(".date-comm time").attr("datetime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME), ZoneId.of("Europe/Paris")));

        try { item.setCover(imageService.getCoverFromURL(new URL(selectedArea.select("meta[itemprop=thumbnail]").attr("content")))); }
        catch (IOException ignored) {}

        return item;
    }

    @Override
    public String signatureOf(Podcast podcast) {
        try {
            Document page = htmlService.connectWithDefault(podcast.getUrl()).get();
            return signatureService.generateMD5Signature(page.select("article").html());
        } catch (IOException e) {
            logger.error("IOException :", e);
        }

        return "";
    }

    @Override
    public Type type() {
        return new Type("JeuxVideoCom", "JeuxVideo.com");
    }
}
