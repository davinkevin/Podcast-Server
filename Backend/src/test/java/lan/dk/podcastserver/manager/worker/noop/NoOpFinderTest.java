package lan.dk.podcastserver.manager.worker.noop;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.noop.NoOpFinder;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 09/03/2016 for Podcast Server
 */
public class NoOpFinderTest {

    private NoOpFinder noOpFinder;

    @Before
    public void beforeEach() {
        noOpFinder = new NoOpFinder();
    }

    @Test
    public void should_find_default_podcast() {
        assertThat(noOpFinder.find("")).isEqualTo(Podcast.DEFAULT_PODCAST);
    }
    
    @Test
    public void should_compatibility() {
        assertThat(noOpFinder.compatibility("")).isEqualTo(-1);
    }
}
