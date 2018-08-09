package lan.dk.podcastserver.manager.worker.noop;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.Finder;
import javax.validation.constraints.NotEmpty;

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
public class NoOpFinder implements Finder {
    @Override
    public Podcast find(String url) {
        return Podcast.DEFAULT_PODCAST;
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return -1;
    }
}
