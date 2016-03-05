package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Service("DailymotionFinder")
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class DailymotionFinder implements Finder {

    public static final String API_URL = "https://api.dailymotion.com/user/%s?fields=avatar_720_url,description,username";
    public static final Pattern USER_NAME_EXTRACTOR = Pattern.compile("^.+dailymotion.com\\/(.*)");

    final JsonService jsonService;
    final ImageService imageService;
    final UrlService urlService;

    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {
        return usernameOf(url)
                .map(username -> String.format(API_URL, username))
                .flatMap(urlService::newURL)
                .flatMap(jsonService::from)
                .map(o -> Podcast.builder()
                        .cover(imageService.getCoverFromURL((String) o.get("avatar_720_url")))
                        .url(url)
                        .title(((String) o.get("username")))
                        .description(((String) o.get("description")))
                        .type("Dailymotion")
                        .build())
                .orElse(Podcast.builder().url(url).type("Dailymotion").cover(new Cover()).build());
    }

    private Optional<String> usernameOf(String url) {
        // http://www.dailymotion.com/karimdebbache
        Matcher matcher = USER_NAME_EXTRACTOR.matcher(url);
        if (matcher.find())
            return Optional.of(matcher.group(1));

        return Optional.empty();
    }

}
