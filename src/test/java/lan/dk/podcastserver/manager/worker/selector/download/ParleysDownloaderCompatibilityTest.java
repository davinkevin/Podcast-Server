package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.ParleysDownloader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ParleysDownloaderCompatibilityTest {

    @Test
    public void should_be_hightly_compatible () {
        /* Given */ ParleysDownloaderCompatibility parleysDownloaderCompatibility = new ParleysDownloaderCompatibility();
        /* When  */ Integer compatibility = parleysDownloaderCompatibility.compatibility("http://www.parleys.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ ParleysDownloaderCompatibility parleysDownloaderCompatibility = new ParleysDownloaderCompatibility();
        /* When  */ Integer compatibility = parleysDownloaderCompatibility.compatibility("http://ma.video.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_M3U8Downloader_class () {
        /* Given */ ParleysDownloaderCompatibility m3U8DownloaderCompatibility = new ParleysDownloaderCompatibility();
        /* When  */ Class clazz = m3U8DownloaderCompatibility.downloader();
        /* Then  */ assertThat(clazz).isEqualTo(ParleysDownloader.class);
    }
}