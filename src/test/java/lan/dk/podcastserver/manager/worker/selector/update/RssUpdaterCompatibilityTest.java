package lan.dk.podcastserver.manager.worker.selector.update;
import lan.dk.podcastserver.manager.worker.updater.RSSUpdater;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
public class RssUpdaterCompatibilityTest {

    @Test
    public void should_always_return_the_max_value () {
        /* Given */ RssUpdaterCompatibility rssUpdateSelector = new RssUpdaterCompatibility();
        /* When */ Integer compatibility = rssUpdateSelector.compatibility("http://fake.url/");
        /* Then */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE-1);
    }

    @Test
    public void should_return_the_rssUpdate_class () {
        /* Given */ RssUpdaterCompatibility rssUpdateSelector = new RssUpdaterCompatibility();
        /* When */ Class clazz = rssUpdateSelector.updater();
        /* Then */ assertThat(clazz).isEqualTo(RSSUpdater.class);
    }
}