package lan.dk.podcastserver.exception;

import java.util.UUID;

/**
 * Created by kevin on 26/01/2014.
 */
public class PodcastNotFoundException extends RuntimeException {

    public PodcastNotFoundException(UUID id) {
        super("Podcast " + id + " not found");
    }

}
