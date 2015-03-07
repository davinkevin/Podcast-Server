package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.CanalPlusUpdater;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CanalPlusUpdaterCompatibilityTest {

    @Test
    public void should_be_hightly_compatible () {
        /* Given */ CanalPlusUpdaterCompatibility canalPlusUpdaterCompatibility = new CanalPlusUpdaterCompatibility();
        /* When */  Integer compatibility = canalPlusUpdaterCompatibility.compatibility("http://www.canalplus.fr/show/for/dummies");
        /* Then */  assertThat(compatibility).isLessThan(5);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ CanalPlusUpdaterCompatibility canalPlusUpdaterCompatibility = new CanalPlusUpdaterCompatibility();
        /* When */  Integer compatibility = canalPlusUpdaterCompatibility.compatibility("http://www.wrong.url/user");
        /* Then */  assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_rssUpdate_class () {
        /* Given */ CanalPlusUpdaterCompatibility canalPlusUpdaterCompatibility = new CanalPlusUpdaterCompatibility();
        /* When */  Class clazz = canalPlusUpdaterCompatibility.updater();
        /* Then */  assertThat(clazz).isEqualTo(CanalPlusUpdater.class);
    }
}