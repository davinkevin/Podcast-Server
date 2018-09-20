package lan.dk.podcastserver.manager.worker.dailymotion;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davinkevin.podcastserver.service.ImageService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import com.jayway.jsonpath.TypeRef;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Type;
import lan.dk.podcastserver.manager.worker.Updater;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;

import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.PatternExtractor;
import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.from;


/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Slf4j
@Component("DailymotionUpdater")
@RequiredArgsConstructor
public class DailymotionUpdater implements Updater {

    static final String API_LIST_OF_ITEMS = "https://api.dailymotion.com/user/%s/videos?fields=created_time,description,id,thumbnail_720_url,title";
    // http://www.dailymotion.com/karimdebbache
    private static final PatternExtractor USER_NAME_EXTRACTOR = from(Pattern.compile("^.+dailymotion.com/(.*)"));
    private static final String ITEM_URL = "http://www.dailymotion.com/video/%s";
    private static final TypeRef<Set<DailymotionVideoDetail>> LIST_DAILYMOTIONVIDEODETAIL_TYPE = new TypeRef<Set<DailymotionVideoDetail>>() { };

    private final SignatureService signatureService;
    private final JsonService jsonService;
    private final ImageService imageService;

    @Override
    public Set<Item> getItems(Podcast podcast) {
        return USER_NAME_EXTRACTOR.on(podcast.getUrl()).group(1)
                .map(username -> String.format(API_LIST_OF_ITEMS, username))
                .flatMap(jsonService::parseUrl)
                .map(p -> p.read("list", LIST_DAILYMOTIONVIDEODETAIL_TYPE))
                .map(this::asSet)
                .getOrElse(HashSet::empty);
    }

    private Set<Item> asSet(Set<DailymotionVideoDetail> jsonArray) {
        return jsonArray
                .map(i -> Item.builder()
                        .url(String.format(ITEM_URL, i.getId()))
                        .cover(imageService.getCoverFromURL(i.getCover()))
                        .title(i.getTitle())
                        .pubDate(ZonedDateTime.ofInstant(Instant.ofEpochSecond(i.getCreationDate()), ZoneId.of("Europe/Paris")))
                        .description((i.getDescription()))
                        .build()
                );
    }

    @Override
    public String signatureOf(Podcast podcast) {
        return USER_NAME_EXTRACTOR.on(podcast.getUrl()).group(1)
                .map(u -> signatureService.fromUrl(String.format(API_LIST_OF_ITEMS, u)))
                .getOrElseThrow(() -> new RuntimeException("Username not Found"));
    }

    @Override
    public Type type() {
        return new Type("Dailymotion", "Dailymotion");
    }

    @Override
    public Integer compatibility(String url) {
        return StringUtils.contains(url, "www.dailymotion.com") ? 1 : Integer.MAX_VALUE;
    }

    private static class DailymotionVideoDetail {
        @Getter @Setter private String id;
        @JsonProperty("thumbnail_720_url") @Getter @Setter private String cover;
        @Getter @Setter private String title;
        @JsonProperty("created_time") @Getter @Setter private Long creationDate;
        @Getter @Setter private String description;

    }
}
