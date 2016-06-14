package lan.dk.podcastserver.manager.worker.finder;

import com.fasterxml.jackson.annotation.JsonProperty;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.UrlService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Service("DailymotionFinder")
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class DailymotionFinder implements Finder {

    private static final String API_URL = "https://api.dailymotion.com/user/%s?fields=avatar_720_url,description,username";
    private static final Pattern USER_NAME_EXTRACTOR = Pattern.compile("^.+dailymotion.com\\/(.*)");

    final JsonService jsonService;
    final ImageService imageService;
    final UrlService urlService;

    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {
        return usernameOf(url)
                .map(username -> String.format(API_URL, username))
                .flatMap(urlService::newURL)
                .flatMap(jsonService::parse)
                .map(json -> json.read("$", DailymotionUserDetail.class))
                .map(d -> jsonToPodcast(url, d))
                .orElse(Podcast.builder().url(url).type("Dailymotion").cover(new Cover()).build());
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

    private Optional<String> usernameOf(String url) {
        // http://www.dailymotion.com/karimdebbache
        Matcher matcher = USER_NAME_EXTRACTOR.matcher(url);
        if (matcher.find())
            return Optional.of(matcher.group(1));

        return Optional.empty();
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
