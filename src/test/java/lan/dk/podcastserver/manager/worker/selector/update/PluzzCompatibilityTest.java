package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.PluzzUpdater;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 07/03/15.
 */
public class PluzzCompatibilityTest {

    @Test
    public void should_be_hightly_compatible () {
        /* Given */ PluzzCompatibility pluzzCompatibility = new PluzzCompatibility();
        /* When */  Integer compatibility = pluzzCompatibility.compatibility("http://www.pluzz.francetv.fr/show/for/dummies");
        /* Then */  assertThat(compatibility).isLessThan(5);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ PluzzCompatibility pluzzCompatibility = new PluzzCompatibility();
        /* When */  Integer compatibility = pluzzCompatibility.compatibility("http://www.wrong.url/user");
        /* Then */  assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_rssUpdate_class () {
        /* Given */ PluzzCompatibility pluzzCompatibility = new PluzzCompatibility();
        /* When */  Class clazz = pluzzCompatibility.updater();
        /* Then */  assertThat(clazz).isEqualTo(PluzzUpdater.class);
    }
}
