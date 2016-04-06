package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
        return htmlService
                .get(podcast.getUrl())
                .map(p -> p.select("article"))
                .map(this::htmlToItems)
                .orElse(Sets.newHashSet());
    }

    private Set<Item> htmlToItems(Elements elements) {
        return elements
                .stream()
                .map(element -> generateItemFromPage(element.select("a").first().attr("href")))
                .collect(toSet());
    }

    private Item generateItemFromPage(String videoPageUrl) {
        return htmlService
                .get(JEUXVIDEOCOM_HOST.concat(videoPageUrl))
                .map(this::htmlToItem)
                .orElse(Item.DEFAULT_ITEM);
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
                .orElse("");
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
