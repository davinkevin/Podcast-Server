package lan.dk.podcastserver.utils.form;

import org.junit.Test;

import java.util.UUID;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;

/**
 * Created by kevin on 15/07/15 for Podcast Server
 */
public class MovingItemInQueueFormTest {

    @Test
    public void should_generate_object() {
        /* Given */
        MovingItemInQueueForm movingItemInQueueForm = new MovingItemInQueueForm();
        UUID id = UUID.randomUUID();

        /* When */
        movingItemInQueueForm.setId(id);
        movingItemInQueueForm.setPosition(2);

        /* Then */
        assertThat(movingItemInQueueForm)
                .hasId(id)
                .hasPosition(2);
    }

}
