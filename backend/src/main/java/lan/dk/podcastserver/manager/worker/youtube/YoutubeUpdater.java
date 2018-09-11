package lan.dk.podcastserver.manager.worker.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.JdomService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.manager.worker.Updater;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.properties.Api;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static io.vavr.API.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Component("YoutubeUpdater")
public class YoutubeUpdater implements Updater {

    private static final Integer MAX_PAGE = 10;

    private static final String CHANNEL_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?channel_id=%s";
    private static final String PLAYLIST_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?playlist_id=%s";
    private static final String PLAYLIST_URL_PART = "www.youtube.com/playlist?list=";
    private static final String URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s";
    private static final String API_PLAYLIST_URL = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=%s&key=%s";
    public static final String YOUTUBE = "Youtube";

    private final SignatureService signatureService;
    private final JdomService jdomService;
    private final JsonService jsonService;
    private final HtmlService htmlService;
    private final Api api;

    private final YoutubeByXmlUpdater youtubeByXmlUpdater;
    private final YoutubeByApiUpdater byApiUpdater;

    public YoutubeUpdater(SignatureService signatureService, JdomService jdomService, JsonService jsonService, HtmlService htmlService, Api api) {
        this.signatureService = signatureService;
        this.jdomService = jdomService;
        this.jsonService = jsonService;
        this.htmlService = htmlService;
        this.api = api;

        youtubeByXmlUpdater = new YoutubeByXmlUpdater(jdomService, htmlService, signatureService);
        byApiUpdater = new YoutubeByApiUpdater(htmlService, jsonService, api);
    }

    public Set<Item> getItems(Podcast podcast) {
        String string = api.getYoutube();
        return string == null || string.length() == 0
                ? youtubeByXmlUpdater.getItems(podcast)
                : byApiUpdater.getItems(podcast);
    }

    private Set<Item> getItemsByAPI(Podcast podcast) {
        log.info("Youtube Update by API");

        String playlistId = isPlaylist(podcast.getUrl()) ? playlistIdOf(podcast.getUrl()) : transformChannelIdToPlaylistId(channelIdOf(podcast.getUrl()));

        String nextPageToken = null;
        Set<Item> items = HashSet.empty();
        Set<Item> pageItems = HashSet.empty();
        Integer page = 0;
        do {
            items = items.addAll(pageItems);

            Option<YoutubeResponse> jsonResponse = jsonService
                    .parseUrl(asApiPlaylistUrl(playlistId, nextPageToken))
                    .map(JsonService.to(YoutubeResponse.class));

            pageItems = jsonResponse.map(YoutubeResponse::getItems)
                    .map(this::convertToItems)
                    .getOrElse(HashSet::empty);

            nextPageToken = jsonResponse.map(YoutubeResponse::getNextPageToken).getOrElse(StringUtils.EMPTY);

        } while(page++ < MAX_PAGE && StringUtils.isNotEmpty(nextPageToken));
        // Can't Access the podcast item here due thread-safe JPA / Hibernate problem
        // So, I choose to limit to 500 item / 10 Page of Youtube

        if (StringUtils.isEmpty(nextPageToken)) {
            items = items.addAll(pageItems);
        }

        return items;
    }


    private String asApiPlaylistUrl(String playlistId, String pageToken) {
        String url = String.format(API_PLAYLIST_URL, playlistId, api.getYoutube());
        return isNull(pageToken) ? url : url.concat("&pageToken=" + pageToken);
    }

    private Set<Item> convertToItems(Set<YoutubeResponse.YoutubeItem> items) {
        return items.map(this::convertToItem);
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
                            .getOrElse(() -> null)
                    )
                .build();
    }

    @Override
    public String signatureOf(Podcast podcast) {
        return youtubeByXmlUpdater.signatureOf(podcast);
    }

    private Option<Document> xmlOf(String url) {
        return jdomService.parse(isPlaylist(url) ? String.format(PLAYLIST_RSS_BASE, playlistIdOf(url)) : String.format(CHANNEL_RSS_BASE, channelIdOf(url)));
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
            .getOrElse(StringUtils.EMPTY);
    }

    @Override
    public Integer compatibility(String url) {
        return List("youtube.com/channel/", "youtube.com/user/", "youtube.com/", "gdata.youtube.com/feeds/api/playlists/")
                .exists(url::contains) ? 1 : Integer.MAX_VALUE;
    }

    @Override
    public Type type() {
        return new Type(YOUTUBE, YOUTUBE);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class YoutubeResponse {
        @Getter @Setter private Set<YoutubeItem> items;
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

            public Option<Thumbnails.Thumbnail> getCover() {
                if (isNull(this.snippet.thumbnails))
                    return None();

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

                Option<Thumbnail> getBetterThumbnail() {
                    if (nonNull(maxres))
                        return Option(maxres);

                    if (nonNull(standard))
                        return Option(standard);

                    if (nonNull(high))
                        return Option(high);

                    if (nonNull(medium))
                        return Option(medium);

                    if (nonNull(byDefault))
                        return Option(byDefault);

                    return None();
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
