package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;

/**
 * Created by kevin on 22/02/15.
 */
public interface Finder {

    public Podcast find(String url) throws FindPodcastNotFoundException;
}
