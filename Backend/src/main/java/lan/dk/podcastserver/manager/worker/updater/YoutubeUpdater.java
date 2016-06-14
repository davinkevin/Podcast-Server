package lan.dk.podcastserver.manager.worker.updater;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.properties.Api;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static lan.dk.podcastserver.entity.Cover.DEFAULT_COVER;

@Slf4j
@Component("YoutubeUpdater")
public class YoutubeUpdater extends AbstractUpdater {

    private static final Namespace MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");
    private static final Integer MAX_PAGE = 10;

    private static final String CHANNEL_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?channel_id=%s";
    private static final String PLAYLIST_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?playlist_id=%s";
    private static final String PLAYLIST_URL_PART = "www.youtube.com/playlist?list=";
    private static final String URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s";
    private static final String API_PLAYLIST_URL = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=%s&key=%s";

    @Resource JdomService jdomService;
    @Resource JsonService jsonService;
    @Resource HtmlService htmlService;
    @Resource UrlService urlService;
    @Autowired Api api;

    public Set<Item> getItems(Podcast podcast) {
        return Strings.isNullOrEmpty(api.getYoutube())
                ? getItemsByRss(podcast)
                : getItemsByAPI(podcast);
    }

    @SuppressWarnings("unchecked")
    private Set<Item> getItemsByAPI(Podcast podcast) {
        log.info("Youtube Update by API");

        String playlistId = isPlaylist(podcast.getUrl()) ? playlistIdOf(podcast.getUrl()) : transformChannelIdToPlaylistId(channelIdOf(podcast.getUrl()));

        String nextPageToken = null;
        Set<Item> items = Sets.newHashSet(), pageItems = Sets.newHashSet();
        Integer page = 0;
        do {
            items.addAll(pageItems);

            Optional<YoutubeResponse> jsonResponse = urlService
                    .newURL(asApiPlaylistUrl(playlistId, nextPageToken))
                    .flatMap(jsonService::parse)
                    .map(d -> d.read("$", YoutubeResponse.class));

            pageItems = jsonResponse
                    .map(YoutubeResponse::getItems)
                    .map(this::convertToItems)
                    .orElse(Sets.newHashSet());

            nextPageToken = jsonResponse.map(YoutubeResponse::getNextPageToken).orElse("");

        } while(page++ < MAX_PAGE && StringUtils.isNotEmpty(nextPageToken));
        // Can't Access the podcast item here due thread-safe JPA / Hibernate problem
        // So, I choose to limit to 500 item / 10 Page of Youtube

        if (StringUtils.isEmpty(nextPageToken)) items.addAll(pageItems);

        return items;
    }


    private String asApiPlaylistUrl(String playlistId, String pageToken) {
        String url = String.format(API_PLAYLIST_URL, playlistId, api.getYoutube());
        return isNull(pageToken) ? url : url.concat("&pageToken=" + pageToken);
    }

    private Set<Item> convertToItems(List<YoutubeResponse.YoutubeItem> items) {
        return items
                .stream()
                .map(this::convertToItem)
                .collect(toSet());
    }

    private String transformChannelIdToPlaylistId(String channelId) {
        return channelId.startsWith("UC") ? channelId.replaceFirst("UC", "UU") : channelId;
    }

    private Item convertToItem(YoutubeResponse.YoutubeItem item) {
        return Item.builder()
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .pubDate(item.getPublishedAt())
                    .url(item.getUrl())
                    .cover(item.getCover()
                            .map(t -> Cover.builder().url(t.getUrl()).width(t.getWidth()).height(t.getHeight()).build())
                            .orElse(DEFAULT_COVER))
                .build();
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
                .setPubDate(pubdateOf(entry.getChildText("published", defaultNamespace)))
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
        return Lists.newArrayList("youtube.com/channel/", "youtube.com/user/", "youtube.com/", "gdata.youtube.com/feeds/api/playlists/")
                .stream()
                .anyMatch(url::contains) ? 1 : Integer.MAX_VALUE;
    }

    @Override
    public Type type() {
        return new Type("Youtube", "Youtube");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class YoutubeResponse {
        @Getter @Setter private List<YoutubeItem> items;
        @Getter @Setter private String nextPageToken;

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class YoutubeItem {
            @Setter private Snippet snippet;

            public String getTitle() {
                return snippet.getTitle();
            }

            public String getDescription() {
                return snippet.getDescription();
            }

            ZonedDateTime getPublishedAt() {
                return ZonedDateTime.parse(snippet.getPublishedAt(), DateTimeFormatter.ISO_DATE_TIME); //2013-12-20T22:30:01.000Z
            }

            public String getUrl() {
                return String.format(URL_PAGE_BASE, snippet.getResourceId().getVideoId());
            }

            public Optional<Thumbnails.Thumbnail> getCover() {
                return this.snippet.getThumbnails().getBetterThumbnail();
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            private static class Snippet {
                @Getter @Setter private String title;
                @Getter @Setter private String description;
                @Getter @Setter private String publishedAt;
                @Getter @Setter private Thumbnails thumbnails;
                @Getter @Setter private ResourceId resourceId;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            private static class ResourceId {
                @Getter @Setter private String videoId;
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            private static class Thumbnails {
                @Setter private Thumbnail maxres;
                @Setter private Thumbnail standard;
                @Setter private Thumbnail high;
                @Setter private Thumbnail medium;
                @Setter @JsonProperty("default") private Thumbnail byDefault;

                Optional<Thumbnail> getBetterThumbnail() {
                    if (nonNull(maxres))
                        return Optional.of(maxres);

                    if (nonNull(standard))
                        return Optional.of(standard);

                    if (nonNull(high))
                        return Optional.of(high);

                    if (nonNull(medium))
                        return Optional.of(medium);

                    if (nonNull(byDefault))
                        return Optional.of(byDefault);

                    return Optional.empty();
                }

                @JsonIgnoreProperties(ignoreUnknown = true)
                private static class Thumbnail {
                    @Getter @Setter private String url;
                    @Getter @Setter private Integer width;
                    @Getter @Setter private Integer height;
                }
            }
        }
    }
}
