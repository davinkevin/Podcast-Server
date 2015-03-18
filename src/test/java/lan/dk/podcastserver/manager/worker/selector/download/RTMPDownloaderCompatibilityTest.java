package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.RTMPDownloader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RTMPDownloaderCompatibilityTest {
    @Test
    public void should_be_hightly_compatible () {
        /* Given */ RTMPDownloaderCompatibility rtmpDownloaderCompatibility = new RTMPDownloaderCompatibility();
        /* When  */ Integer compatibility = rtmpDownloaderCompatibility.compatibility("rtmp://ma.vide.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ RTMPDownloaderCompatibility rtmpDownloaderCompatibility = new RTMPDownloaderCompatibility();
        /* When  */ Integer compatibility = rtmpDownloaderCompatibility.compatibility("http://ma.vide.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isGreaterThan(1);
    }

    @Test
    public void should_return_the_BeInSportUpdate_class () {
        /* Given */ RTMPDownloaderCompatibility rtmpDownloaderCompatibility = new RTMPDownloaderCompatibility();
        /* When  */ Class clazz = rtmpDownloaderCompatibility.downloader();
        /* Then  */ assertThat(clazz).isEqualTo(RTMPDownloader.class);
    }
}