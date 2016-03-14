package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
public class NoOpFinder implements Finder {
    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {
        return Podcast.DEFAULT_PODCAST;
    }

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return -1;
    }
}
