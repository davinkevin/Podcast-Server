package lan.dk.podcastserver.manager.worker.dailymotion;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davinkevin.podcastserver.service.ImageService;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Finder;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;
import java.util.regex.Pattern;

import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.PatternExtractor;
import static com.github.davinkevin.podcastserver.utils.MatcherExtractor.from;
import static java.util.Objects.nonNull;

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Service("DailymotionFinder")
@RequiredArgsConstructor
public class DailymotionFinder implements Finder {

    private static final String API_URL = "https://api.dailymotion.com/user/%s?fields=avatar_720_url,description,username";
    private static final PatternExtractor USER_NAME_EXTRACTOR = from(Pattern.compile("^.+dailymotion.com/(.*)"));

    private final JsonService jsonService;
    private final ImageService imageService;

    @Override
    public Podcast find(String url) {
        // http://www.dailymotion.com/karimdebbache
        return USER_NAME_EXTRACTOR.on(url).group(1)
                .map(username -> String.format(API_URL, username))
                .flatMap(jsonService::parseUrl)
                .map(json -> json.read("$", DailymotionUserDetail.class))
                .map(d -> jsonToPodcast(url, d))
                .getOrElse(() -> Podcast.builder().url(url).type("Dailymotion").cover(Cover.DEFAULT_COVER).build());
    }

    private Podcast jsonToPodcast(String url, DailymotionUserDetail detail) {
        return Podcast.builder()
                .cover(imageService.getCoverFromURL(detail.getAvatar()))
                .url(url)
                .title(detail.getUsername())
                .description(detail.getDescription())
                .type("Dailymotion")
                .build();
    }

    private static class DailymotionUserDetail {
        @JsonProperty("avatar_720_url") @Setter @Getter private String avatar;
        @Setter @Getter private String username;
        @Setter @Getter private String description;
    }

    public Integer compatibility(@NotEmpty String url) {
        return nonNull(url) && url.contains("www.dailymotion.com") ? 1 : Integer.MAX_VALUE;
    }

}
