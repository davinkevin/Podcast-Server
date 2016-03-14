package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Component("YoutubeUpdater")
public class YoutubeUpdater extends AbstractUpdater {

    private static final Namespace MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");

    private static final String CHANNEL_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?channel_id=%s";
    private static final String PLAYLIST_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?playlist_id=%s";
    private static final String PLAYLIST_URL_PART = "www.youtube.com/playlist?list=";
    private static final String URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s";
    private static final String API_PLAYLIST_URL = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=%s&key=%s";

    @Resource JdomService jdomService;
    @Resource JsonService jsonService;
    @Resource HtmlService htmlService;
    @Resource UrlService urlService;

    public Set<Item> getItems(Podcast podcast) {
        return Strings.isNullOrEmpty(podcastServerParameters.api().youtube())
                ? getItemsByRss(podcast)
                : getItemsByAPI(podcast);
    }

    @SuppressWarnings("unchecked")
    private Set<Item> getItemsByAPI(Podcast podcast) {
        log.info("Youtube Update by API");

        String playlistId = isPlaylist(podcast.getUrl()) ? playlistIdOf(podcast.getUrl()) : transformChannelIdToPlaylistId(channelIdOf(podcast.getUrl()));
        String url = String.format(API_PLAYLIST_URL, playlistId, podcastServerParameters.api().youtube());

        return urlService
                .newURL(url)
                .flatMap(jsonService::from)
                .map(r -> (List<JSONObject>) r.get("items"))
                .map(this::jsonArrayToItems)
                .orElse(Sets.newHashSet());
    }

    private Set<Item> jsonArrayToItems(List<JSONObject> jsonObjects) {
        return jsonObjects
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
        return Item.builder()
                .title(snippet.get("title").toString())
                .description(snippet.get("description").toString())
                .pubdate(pubdateOf(snippet.get("publishedAt").toString()))
                .url(String.format(URL_PAGE_BASE, resourceId.get("videoId")))
                .cover(coverFromJson(((JSONObject) snippet.get("thumbnails"))))
                .build();
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
        log.info("Youtube Update by RSS");

        Optional<Element> element = xmlOf(podcast.getUrl()).map(Document::getRootElement);

        return element
            .map(d -> d.getChildren("entry", d.getNamespace()))
            .map(entry -> this.xmlToItems(entry, element.map(Element::getNamespace).get()))
            .orElse(Sets.newHashSet());
    }

    private Set<Item> xmlToItems(List<Element> entry, Namespace defaultNamespace) {
        return entry
                .stream()
                .map(elem -> generateItemFromElement(elem, defaultNamespace))
                .collect(toSet());
    }

    @Override
    public String signatureOf(Podcast podcast) {
        Optional<Element> element = xmlOf(podcast.getUrl())
                .map(Document::getRootElement);

        return element
                .map(d -> d.getChildren("entry", d.getNamespace()))
                .map(entries -> entries.stream().map(elem -> elem.getChildText("id", element.map(Element::getNamespace).get())).collect(joining()))
                .map(signatureService::generateMD5Signature)
                .orElse("");
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

    private Optional<Document> xmlOf(String url) {
        return urlService
                    .newURL(isPlaylist(url) ? String.format(PLAYLIST_RSS_BASE, playlistIdOf(url)) : String.format(CHANNEL_RSS_BASE, channelIdOf(url)))
                    .flatMap(jdomService::parse);
    }

    private String playlistIdOf(String url) {
        // TODO  : Use Pattern Match to extract PlaylistID in Feed case and url case
        return StringUtils.substringAfter(url, "list=");
    }

    private Boolean isPlaylist(String url) {
        return nonNull(url) && url.contains(PLAYLIST_URL_PART);
    }

    private String channelIdOf(String url) {
        return htmlService
            .get(url)
            .map(p -> p.select("[data-channel-external-id]").first())
            .filter(Objects::nonNull)
            .map(e -> e.attr("data-channel-external-id"))
            .orElse("");
    }

    @Override
    public Integer compatibility(String url) {
        return Arrays
                .asList("youtube.com/channel/", "youtube.com/user/", "youtube.com/", "gdata.youtube.com/feeds/api/playlists/")
                .stream().anyMatch(url::contains)
                ? 1
                : Integer.MAX_VALUE;
    }

    @Override
    public Type type() {
        return new Type("Youtube", "Youtube");
    }
}
