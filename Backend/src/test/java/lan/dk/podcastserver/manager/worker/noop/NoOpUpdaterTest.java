package lan.dk.podcastserver.manager.worker.noop;

import lan.dk.podcastserver.manager.worker.Updater;
import lan.dk.podcastserver.manager.worker.noop.NoOpUpdater;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 17/03/2016 for Podcast Server
 */
public class NoOpUpdaterTest {

    @Test
    public void should_return_default_value() {
        /* Given */
        /* When */
        NoOpUpdater noOpUpdater = new NoOpUpdater();

        /* Then */
        assertThat(noOpUpdater.update(null)).isEqualTo(Updater.NO_MODIFICATION_TUPLE);
        assertThat(noOpUpdater.getItems(null)).isEmpty();
        assertThat(noOpUpdater.signatureOf(null)).isNull();
        assertThat(noOpUpdater.notIn(null).test(null)).isFalse();
        assertThat(noOpUpdater.type()).isNull();
        assertThat(noOpUpdater.compatibility(null)).isEqualTo(Integer.MAX_VALUE);
    }
}
