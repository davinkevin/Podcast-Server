package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.xml.JdomService;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Component("YoutubeUpdater")
public class YoutubeUpdater extends AbstractUpdater {

    private static final String FEED_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?channel_id=%s";
    public static final Namespace MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");
    private static final String URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s";

    @Resource JdomService jdomService;

    public static ZonedDateTime fromYoutube(String pubDate) {
        return ZonedDateTime.parse(pubDate, DateTimeFormatter.ISO_DATE_TIME); //2013-12-20T22:30:01.000Z
    }

    public Set<Item> getItems(Podcast podcast) {
        Set<Item> itemSet = new HashSet<>();

        Document podcastXMLSource;
        try {
            podcastXMLSource = xmlChannelOf(podcast.getUrl());
        } catch (JDOMException | IOException e) {
            logger.error("Error during youtube signature & parsing", e);
            return itemSet;
        }

        Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();

        for (Element entry : podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace)) {

            Element mediaGroup = entry.getChild("group", MEDIA_NAMESPACE);
            Item itemToAdd = new Item()
                    .setTitle(entry.getChildText("title", defaultNamespace))
                    .setPubdate(fromYoutube(entry.getChildText("published", defaultNamespace)))
                    .setDescription(mediaGroup.getChildText("description", MEDIA_NAMESPACE))
                    .setUrl(youtubeVideoPage(mediaGroup.getChild("content", MEDIA_NAMESPACE).getAttributeValue("url")));


            Element thumbnail = mediaGroup.getChild("thumbnail", MEDIA_NAMESPACE);
            if (thumbnail != null) {
                Cover cover = new Cover(thumbnail.getAttributeValue("url"), Integer.valueOf(thumbnail.getAttributeValue("width")), Integer.valueOf(thumbnail.getAttributeValue("height")));
                itemToAdd.setCover(cover);
            }

            itemSet.add(itemToAdd);
        }
        return itemSet;
    }

    private String youtubeVideoPage(String embeddedVideoPage) {
        String idVideo = StringUtils.substringBefore(StringUtils.substringAfterLast(embeddedVideoPage, "/"), "?");
        return String.format(URL_PAGE_BASE, idVideo);
    }

    @Override
    public String generateSignature(Podcast podcast) {

        Document podcastXMLSource = null;
        try {
            podcastXMLSource = xmlChannelOf(podcast.getUrl());
        } catch (JDOMException | IOException e) {
            logger.error("Error during youtube signature & parsing", e);
            return "";
        }

        Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();

        if (podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace).get(0) != null) {
            return signatureService.generateMD5Signature(podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace).get(0).getChildText("published", defaultNamespace));
        }
        return "";
    }
    private Document xmlChannelOf(String url) throws JDOMException, IOException {
        String channelId = getChannelId(url);
        return jdomService.jdom2Parse(String.format(FEED_RSS_BASE, channelId));
    }

    private String getChannelId(String url) {
        org.jsoup.nodes.Document page;

        try {
            page = Jsoup.connect(url)
                    .timeout(5000)
                    .userAgent(USER_AGENT)
                    .referrer("http://www.google.fr")
                    .execute().parse();
        } catch (IOException e) {
            logger.error("IOException :", e);
            return "";
        }

        org.jsoup.nodes.Element elementWithExternalId = page.select("[data-channel-external-id]").first();
        if (elementWithExternalId != null) {
            return elementWithExternalId.attr("data-channel-external-id");
        }

        return "";
    }
}
