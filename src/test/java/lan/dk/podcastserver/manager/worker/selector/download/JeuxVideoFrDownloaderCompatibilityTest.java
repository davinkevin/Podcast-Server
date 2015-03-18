package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.JeuxVideoFrDownloader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JeuxVideoFrDownloaderCompatibilityTest {

    @Test
    public void should_be_hightly_compatible () {
        /* Given */ JeuxVideoFrDownloaderCompatibility jeuxVideoFrDownloaderCompatibility = new JeuxVideoFrDownloaderCompatibility();
        /* When  */ Integer compatibility = jeuxVideoFrDownloaderCompatibility.compatibility("http://www.jeuxvideo.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ JeuxVideoFrDownloaderCompatibility jeuxVideoFrDownloaderCompatibility = new JeuxVideoFrDownloaderCompatibility();
        /* When  */ Integer compatibility = jeuxVideoFrDownloaderCompatibility.compatibility("http://ma.video.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_JeuxVideoFrDownloader_class () {
        /* Given */ JeuxVideoFrDownloaderCompatibility jeuxVideoFrDownloaderCompatibility = new JeuxVideoFrDownloaderCompatibility();
        /* When  */ Class clazz = jeuxVideoFrDownloaderCompatibility.downloader();
        /* Then  */ assertThat(clazz).isEqualTo(JeuxVideoFrDownloader.class);
    }
}