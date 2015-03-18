package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.HTTPDownloader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HTTPDownloaderCompatibilityTest {

    @Test
    public void should_be_hightly_compatible () {
        /* Given */ HTTPDownloaderCompatibility httpDownloaderCompatibility = new HTTPDownloaderCompatibility();
        /* When  */ Integer compatibility = httpDownloaderCompatibility.compatibility("http://ma.vide.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE-1);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ HTTPDownloaderCompatibility httpDownloaderCompatibility = new HTTPDownloaderCompatibility();
        /* When  */ Integer compatibility = httpDownloaderCompatibility.compatibility("rtmp://ma.vide.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_BeInSportUpdate_class () {
        /* Given */ HTTPDownloaderCompatibility httpDownloaderCompatibility = new HTTPDownloaderCompatibility();
        /* When */ Class clazz = httpDownloaderCompatibility.downloader();
        /* Then */ assertThat(clazz).isEqualTo(HTTPDownloader.class);
    }

}