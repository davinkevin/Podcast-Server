package lan.dk.podcastserver.utils.facade;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 01/07/15 for Podcast Server
 */
public class UpdateTupleTest {


    @Test
    public void should_generate_a_tuple() {
        /* Given */
        UpdateTuple<String, Integer, Long> tuple = UpdateTuple.of("First", 2, 3L);

        /* When */
        String first = tuple.first();
        Integer second = tuple.second();
        Integer middle = tuple.middle();
        Long third = tuple.third();
        Long last = tuple.last();

        /* Then */
        assertThat(first).isEqualTo("First");
        assertThat(second).isSameAs(middle).isEqualTo(2);
        assertThat(third).isSameAs(last).isEqualTo(3L);
    }
}