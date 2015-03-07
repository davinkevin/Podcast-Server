package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.ParleysUpdater;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 07/03/15.
 */
public class ParleysCompatibilityTest {
    
    @Test
    public void should_be_hightly_compatible () {
        /* Given */ ParleysCompatibility parleysCompatibility = new ParleysCompatibility();
        /* When */  Integer compatibility = parleysCompatibility.compatibility("http://www.parleys.com/show/for/dummies");
        /* Then */  assertThat(compatibility).isLessThan(5);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ ParleysCompatibility parleysCompatibility = new ParleysCompatibility();
        /* When */  Integer compatibility = parleysCompatibility.compatibility("http://www.wrong.url/user");
        /* Then */  assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_rssUpdate_class () {
        /* Given */ ParleysCompatibility jeuxVideoFrCompatibility = new ParleysCompatibility();
        /* When */  Class clazz = jeuxVideoFrCompatibility.updater();
        /* Then */  assertThat(clazz).isEqualTo(ParleysUpdater.class);
    }
}
