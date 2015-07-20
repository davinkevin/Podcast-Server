package lan.dk.podcastserver.utils.form;

import org.junit.Test;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;

/**
 * Created by kevin on 15/07/15 for Podcast Server
 */
public class MovingItemInQueueFormTest {

    @Test
    public void should_generate_object() {
        /* Given */
        MovingItemInQueueForm movingItemInQueueForm = new MovingItemInQueueForm();

        /* When */
        movingItemInQueueForm.setId(1);
        movingItemInQueueForm.setPosition(2);

        /* Then */
        assertThat(movingItemInQueueForm)
                .hasId(1)
                .hasPosition(2);
    }

}