package lan.dk.podcastserver.manager.worker;

import lan.dk.podcastserver.manager.worker.Type;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
public class TypeTest {

    @Test
    public void should_have_key_and_name() {
        /* Given */ Type type = new Type("Key", "Value");
        /* Then */
        assertThat(type.key()).isEqualTo("Key");
        assertThat(type.name()).isEqualTo("Value");
    }

}
