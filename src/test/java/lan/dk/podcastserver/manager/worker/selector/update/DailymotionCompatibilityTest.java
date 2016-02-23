package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.DailymotionUpdater;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 22/02/2016 for Podcast Server
 */
public class DailymotionCompatibilityTest {

    DailymotionCompatibility dailymotionCompatibility;

    @Before
    public void beforeEach() {
        /* Given */
        dailymotionCompatibility = new DailymotionCompatibility();
    }

    @Test
    public void should_be_hightly_compatible () {
        /* When */  Integer compatibility = dailymotionCompatibility.compatibility("http://www.dailymotion.com/show/for/dummies");
        /* Then */  assertThat(compatibility).isLessThan(5);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* When */  Integer compatibility = dailymotionCompatibility.compatibility("http://www.wrong.url/user");
        /* Then */  assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_rssUpdate_class () {
        /* When */  Class clazz = dailymotionCompatibility.updater();
        /* Then */  assertThat(clazz).isEqualTo(DailymotionUpdater.class);
    }
}