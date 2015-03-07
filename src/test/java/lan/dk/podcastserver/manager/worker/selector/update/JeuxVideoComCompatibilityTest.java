package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.JeuxVideoComUpdater;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 07/03/15.
 */
public class JeuxVideoComCompatibilityTest {
    @Test
    public void should_be_hightly_compatible () {
        /* Given */ JeuxVideoComCompatibility jeuxVideoComCompatibility = new JeuxVideoComCompatibility();
        /* When */  Integer compatibility = jeuxVideoComCompatibility.compatibility("http://www.jeuxvideo.com/show/for/dummies");
        /* Then */  assertThat(compatibility).isLessThan(5);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ JeuxVideoComCompatibility jeuxVideoComCompatibility = new JeuxVideoComCompatibility();
        /* When */  Integer compatibility = jeuxVideoComCompatibility.compatibility("http://www.wrong.url/user");
        /* Then */  assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_rssUpdate_class () {
        /* Given */ JeuxVideoComCompatibility jeuxVideoFrCompatibility = new JeuxVideoComCompatibility();
        /* When */  Class clazz = jeuxVideoFrCompatibility.updater();
        /* Then */  assertThat(clazz).isEqualTo(JeuxVideoComUpdater.class);
    }
}
