package lan.dk.podcastserver.exception;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
public class PodcastNotFoundExceptionTest {

    @Test
    public void should_have_coherent_object() {
        assertThat(new PodcastNotFoundException())
                .isOfAnyClassIn(PodcastNotFoundException.class);
    }
}