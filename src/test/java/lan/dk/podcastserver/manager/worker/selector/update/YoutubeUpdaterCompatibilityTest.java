package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.YoutubeUpdater;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class YoutubeUpdaterCompatibilityTest {

    @Test
    public void should_be_hightly_compatible () {
        /* Given */ YoutubeUpdaterCompatibility youtubeUpdateSelector = new YoutubeUpdaterCompatibility();
        /* When */ Integer compatibility = youtubeUpdateSelector.compatibility("http://www.youtube.com/user/fakeUser");
        /* Then */ assertThat(compatibility).isLessThan(5);
    }
    
    @Test
    public void should_be_weakly_compatible () {
        /* Given */ YoutubeUpdaterCompatibility youtubeUpdateSelector = new YoutubeUpdaterCompatibility();
        /* When */ Integer compatibility = youtubeUpdateSelector.compatibility("http://www.wrong.url/user");
        /* Then */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_rssUpdate_class () {
        /* Given */ YoutubeUpdaterCompatibility youtubeUpdateSelector = new YoutubeUpdaterCompatibility();
        /* When */ Class clazz = youtubeUpdateSelector.updater();
        /* Then */ assertThat(clazz).isEqualTo(YoutubeUpdater.class);
    }

}