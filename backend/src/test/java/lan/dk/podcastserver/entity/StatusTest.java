package lan.dk.podcastserver.entity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 14/06/15 for HackerRank problem
 */
public class StatusTest {

    @Test
    public void should_check_value() {
        assertThat(Status.of("NOT_DOWNLOADED")).isEqualTo(Status.NOT_DOWNLOADED);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception() {
        assertThat(Status.of("")).isNull();
    }
}
