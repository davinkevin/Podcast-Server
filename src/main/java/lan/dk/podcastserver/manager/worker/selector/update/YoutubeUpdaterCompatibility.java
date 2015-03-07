package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.YoutubeUpdater;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Created by kevin on 06/03/15.
 */
@Component
public class YoutubeUpdaterCompatibility implements UpdaterCompatibility<YoutubeUpdater> {

    @Override
    public Integer compatibility(String url) {
        return Arrays
                .asList("youtube.com/channel/", "youtube.com/user/", "youtube.com/", "gdata.youtube.com/feeds/api/playlists/")
                .stream().anyMatch(url::contains) 
                    ? 1 
                    : Integer.MAX_VALUE;
    }

    @Override
    public Class<YoutubeUpdater> updater() {
        return YoutubeUpdater.class;
    }
}
