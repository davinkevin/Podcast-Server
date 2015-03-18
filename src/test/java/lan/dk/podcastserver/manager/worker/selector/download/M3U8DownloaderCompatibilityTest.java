package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.M3U8Downloader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class M3U8DownloaderCompatibilityTest {

    @Test
    public void should_be_hightly_compatible () {
        /* Given */ M3U8DownloaderCompatibility m3U8DownloaderCompatibility = new M3U8DownloaderCompatibility();
        /* When  */ Integer compatibility = m3U8DownloaderCompatibility.compatibility("http://www.test.fr/video.m3u8");
        /* Then  */ assertThat(compatibility).isEqualTo(10);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ M3U8DownloaderCompatibility m3U8DownloaderCompatibility = new M3U8DownloaderCompatibility();
        /* When  */ Integer compatibility = m3U8DownloaderCompatibility.compatibility("http://ma.video.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_M3U8Downloader_class () {
        /* Given */ M3U8DownloaderCompatibility m3U8DownloaderCompatibility = new M3U8DownloaderCompatibility();
        /* When  */ Class clazz = m3U8DownloaderCompatibility.downloader();
        /* Then  */ assertThat(clazz).isEqualTo(M3U8Downloader.class);
    }

}