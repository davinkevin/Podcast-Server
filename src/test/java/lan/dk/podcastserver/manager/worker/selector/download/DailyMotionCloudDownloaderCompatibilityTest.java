package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.DailyMotionCloudDownloader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DailyMotionCloudDownloaderCompatibilityTest {
    @Test
    public void should_be_hightly_compatible () {
        /* Given */ DailyMotionCloudDownloaderCompatibility dailyMotionCloudDownloaderCompatibility = new DailyMotionCloudDownloaderCompatibility();
        /* When  */ Integer compatibility = dailyMotionCloudDownloaderCompatibility.compatibility("http://cdn.dmcloud.com/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ DailyMotionCloudDownloaderCompatibility dailyMotionCloudDownloaderCompatibility = new DailyMotionCloudDownloaderCompatibility();
        /* When  */ Integer compatibility = dailyMotionCloudDownloaderCompatibility.compatibility("http://ma.vide.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_BeInSportUpdate_class () {
        /* Given */ DailyMotionCloudDownloaderCompatibility dailyMotionCloudDownloaderCompatibility = new DailyMotionCloudDownloaderCompatibility();
        /* When  */ Class clazz = dailyMotionCloudDownloaderCompatibility.downloader();
        /* Then  */ assertThat(clazz).isEqualTo(DailyMotionCloudDownloader.class);
    }
}