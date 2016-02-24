package lan.dk.podcastserver.manager.worker.selector.find;

import lan.dk.podcastserver.manager.worker.finder.YoutubeFinder;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Component
public class YoutubeFinderCompatibility implements FinderCompatibility<YoutubeFinder> {

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return isYoutubeUrl(url, "youtube.com/channel/", "youtube.com/user/", "youtube.com/", "gdata.youtube.com/feeds/api/playlists/")
                ? 1
                : Integer.MAX_VALUE;
    }

    @Override
    public Class<YoutubeFinder> finder() {
        return YoutubeFinder.class;
    }

    private Boolean isYoutubeUrl(String url, String ... strings) {
        return Arrays.stream(strings)
                .anyMatch(url::contains);
    }
}
