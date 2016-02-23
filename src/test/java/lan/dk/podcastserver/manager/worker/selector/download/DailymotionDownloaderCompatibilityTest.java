package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.DailymotionDownloader;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 22/02/2016 for Podcast Server
 */
public class DailymotionDownloaderCompatibilityTest {

    DailymotionDownloaderCompatibility dailymotionDownloaderCompatibility;

    @Before
    public void beforeEach() {
        /* Given */
        dailymotionDownloaderCompatibility = new DailymotionDownloaderCompatibility();
    }

    @Test
    public void should_be_hightly_compatible () {
        /* When  */ Integer compatibility = dailymotionDownloaderCompatibility.compatibility("http://dailymotion.com/video/foobar");
        /* Then  */ assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* When  */ Integer compatibility = dailymotionDownloaderCompatibility.compatibility("https://ma.vide.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_BeInSportUpdate_class () {
        /* When */ Class clazz = dailymotionDownloaderCompatibility.downloader();
        /* Then */ assertThat(clazz).isEqualTo(DailymotionDownloader.class);
    }

}