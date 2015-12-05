package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.UrlService;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

@Component("YoutubeUpdater")
public class YoutubeUpdater extends AbstractUpdater {

    private static final Namespace MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");

    private static final String CHANNEL_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?channel_id=%s";
    private static final String PLAYLIST_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?playlist_id=%s";
    private static final String PLAYLIST_URL_PART = "www.youtube.com/playlist?list=";
    private static final String URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s";
    private static final String API_PLAYLIST_URL = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=%s&key=%s";

    @Resource JdomService jdomService;
    @Resource HtmlService htmlService;
    @Resource UrlService urlService;
    private JSONParser parser = new JSONParser();


    public Set<Item> getItems(Podcast podcast) {
        return Strings.isNullOrEmpty(podcastServerParameters.api().youtube())
                ? getItemsByRss(podcast)
                : getItemsByAPI(podcast);
    }

    @SuppressWarnings("unchecked")
    private Set<Item> getItemsByAPI(Podcast podcast) {
        logger.info("Youtube Update by API");

        String playlistId = isPlaylist(podcast.getUrl()) ? playlistIdOf(podcast.getUrl()) : transformChannelIdToPlaylistId(channelIdOf(podcast.getUrl()));
        String url = String.format(API_PLAYLIST_URL, playlistId, podcastServerParameters.api().youtube());
        JSONObject response;
        try {
            response = JSONObject.class.cast(parser.parse(urlService.getReaderFromURL(url)));
        } catch (ParseException | IOException | Error e) {
            logger.error("Error during fetching of API Playlist {} => {}", podcast.getTitle(), url, e);
            return Sets.newHashSet();
        }

        return ((List<JSONObject>) response.get("items"))
                .stream()
                .map(this::generateItemFromJson)
                .collect(toSet());
    }

    private String transformChannelIdToPlaylistId(String channelId) {
        return channelId.startsWith("UC") ? channelId.replaceFirst("UC", "UU") : channelId;
    }

    private Item generateItemFromJson(JSONObject item) {
        JSONObject snippet = JSONObject.class.cast(item.get("snippet"));
        JSONObject resourceId = JSONObject.class.cast(snippet.get("resourceId"));
        return new Item()
                .setTitle(snippet.get("title").toString())
                .setDescription(snippet.get("description").toString())
                .setPubdate(pubdateOf(snippet.get("publishedAt").toString()))
                .setUrl(String.format(URL_PAGE_BASE, resourceId.get("videoId")))
                .setCover(coverFromJson(((JSONObject) snippet.get("thumbnails"))));
    }

    private Cover coverFromJson(JSONObject thumbnails) {
        return getBetterThumbnails(thumbnails)
                .map(c -> new Cover(c.get("url").toString(), Integer.valueOf(c.get("width").toString()), Integer.valueOf(c.get("height").toString())))
                .orElse(new Cover());
    }

    private Optional<JSONObject> getBetterThumbnails(JSONObject thumbnails) {
        if (thumbnails.containsKey("maxres"))
            return Optional.of((JSONObject) thumbnails.get("maxres"));

        if (thumbnails.containsKey("standard"))
            return Optional.of((JSONObject) thumbnails.get("standard"));

        if (thumbnails.containsKey("high"))
            return Optional.of((JSONObject) thumbnails.get("high"));

        if (thumbnails.containsKey("medium"))
            return Optional.of((JSONObject) thumbnails.get("medium"));

        if (thumbnails.containsKey("default"))
            return Optional.of((JSONObject) thumbnails.get("default"));

        return Optional.empty();
    }

    private Set<Item> getItemsByRss(Podcast podcast) {
        logger.info("Youtube Update by RSS");
        Document podcastXMLSource;
        try {
            podcastXMLSource = xmlOf(podcast.getUrl());
        } catch (JDOMException | IOException e) {
            logger.error("Error during youtube parsing {} => {}", podcast.getTitle(), podcast.getUrl(), e);
            return Sets.newHashSet();
        }

        Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();

        return podcastXMLSource
                .getRootElement()
                .getChildren("entry", defaultNamespace)
                .stream()
                .map(elem -> generateItemFromElement(elem, defaultNamespace))
                .collect(toSet());
    }

    @Override
    public String signatureOf(Podcast podcast) {
        try {
            Document podcastXMLSource = xmlOf(podcast.getUrl());

            Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();
            String stringToSign = podcastXMLSource
                    .getRootElement()
                    .getChildren("entry", defaultNamespace)
                    .stream()
                    .map(elem -> elem.getChildText("id", defaultNamespace))
                    .collect(joining());

            return signatureService.generateMD5Signature(stringToSign);
        } catch (JDOMException | IOException e) {
            logger.error("Error during youtube signature & parsing {} => {}", podcast.getTitle(), podcast.getUrl(), e);
            return "";
        }

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
        return nonNull(thumbnail)
                ? new Cover(thumbnail.getAttributeValue("url"), Integer.valueOf(thumbnail.getAttributeValue("width")), Integer.valueOf(thumbnail.getAttributeValue("height")))
                : null;
    }

    private String urlOf(String embeddedVideoPage) {
        String idVideo = StringUtils.substringBefore(StringUtils.substringAfterLast(embeddedVideoPage, "/"), "?");
        return String.format(URL_PAGE_BASE, idVideo);
    }

    private Document xmlOf(String url) throws JDOMException, IOException {
        if (isPlaylist(url)) {
            return jdomService.parse(String.format(PLAYLIST_RSS_BASE, playlistIdOf(url)));
        }

        return jdomService.parse(String.format(CHANNEL_RSS_BASE, channelIdOf(url)));
    }

    private String playlistIdOf(String url) {
        // TODO  : Use Pattern Match to extract PlaylistID in Feed case and url case
        return StringUtils.substringAfter(url, "list=");
    }

    private Boolean isPlaylist(String url) {
        return nonNull(url) && url.contains(PLAYLIST_URL_PART);
    }

    private String channelIdOf(String url) {
        org.jsoup.nodes.Document page;

        try {
            page = htmlService.connectWithDefault(url).get();
        } catch (IOException e) {
            logger.error("IOException : {}", url, e);
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
