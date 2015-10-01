package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.JdomService;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;

@Component("YoutubeUpdater")
public class YoutubeUpdater extends AbstractUpdater {

    private static final String CHANNEL_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?channel_id=%s";
    private static final String PLAYLIST_RSS_PART = "www.youtube.com/feeds/videos.xml?playlist_id";
    private static final Namespace MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");
    private static final String URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s";

    @Resource JdomService jdomService;
    @Resource HtmlService htmlService;

    public Set<Item> getItems(Podcast podcast) {
        Document podcastXMLSource;
        try {
            podcastXMLSource = xmlOf(podcast.getUrl());
        } catch (JDOMException | IOException e) {
            logger.error("Error during youtube parsing", e);
            return new HashSet<>();
        }

        Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();

        return podcastXMLSource
                .getRootElement()
                .getChildren("entry", defaultNamespace)
                .stream()
                .map(elem -> generateItemFromElement(elem, defaultNamespace))
                .collect(toSet());
    }

    private Item generateItemFromElement(Element entry, Namespace defaultNamespace) {
        Element mediaGroup = entry.getChild("group", MEDIA_NAMESPACE);
        return new Item()
                .setTitle(entry.getChildText("title", defaultNamespace))
                .setDescription(mediaGroup.getChildText("description", MEDIA_NAMESPACE))
                .setPubdate(pubdateOf(entry.getChildText("published", defaultNamespace)))
                .setUrl(urlOf(mediaGroup.getChild("content", MEDIA_NAMESPACE).getAttributeValue("url")))
                .setCover(coverOf(mediaGroup.getChild("thumbnail", MEDIA_NAMESPACE)));
    }

    private ZonedDateTime pubdateOf(String pubDate) {
        return ZonedDateTime.parse(pubDate, DateTimeFormatter.ISO_DATE_TIME); //2013-12-20T22:30:01.000Z
    }

    private Cover coverOf(Element thumbnail) {
        return thumbnail != null
                ? new Cover(thumbnail.getAttributeValue("url"), Integer.valueOf(thumbnail.getAttributeValue("width")), Integer.valueOf(thumbnail.getAttributeValue("height")))
                : null;
    }

    private String urlOf(String embeddedVideoPage) {
        String idVideo = StringUtils.substringBefore(StringUtils.substringAfterLast(embeddedVideoPage, "/"), "?");
        return String.format(URL_PAGE_BASE, idVideo);
    }

    @Override
    public String signatureOf(Podcast podcast) {
        try {
            Document podcastXMLSource = xmlOf(podcast.getUrl());
            return signatureService.generateMD5Signature(new XMLOutputter().outputString(podcastXMLSource.getRootElement()));
        } catch (JDOMException | IOException e) {
            logger.error("Error during youtube signature & parsing", e);
            return "";
        }

    }

    private Document xmlOf(String url) throws JDOMException, IOException {
        if (isPlaylist(url)) {
            return jdomService.parse(url);
        }

        String channelId = getChannelId(url);
        return jdomService.parse(String.format(CHANNEL_RSS_BASE, channelId));
    }

    private Boolean isPlaylist(String url) {
        return nonNull(url) && url.contains(PLAYLIST_RSS_PART);
    }

    private String getChannelId(String url) {
        org.jsoup.nodes.Document page;

        try {
            page = htmlService.connectWithDefault(url).get();
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

    @Override
    public Type type() {
        return new Type("Youtube", "Youtube");
    }
}
