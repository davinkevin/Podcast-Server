package lan.dk.podcastserver.exception;

import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
public class PodcastNotFoundExceptionTest {

    @Test
    public void should_have_coherent_object() {
        UUID id = UUID.randomUUID();
        PodcastNotFoundException exception = new PodcastNotFoundException(id);

        assertThat(exception).isOfAnyClassIn(PodcastNotFoundException.class);
        assertThat(exception.getMessage()).isEqualTo("Podcast " + id + " not found");
    }
}
