package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.YoutubeDownloader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class YoutubeDownloaderCompatibilityTest {

    @Test
    public void should_be_hightly_compatible () {
        /* Given */ YoutubeDownloaderCompatibility youtubeDownloaderCompatibility = new YoutubeDownloaderCompatibility();
        /* When  */ Integer compatibility = youtubeDownloaderCompatibility.compatibility("http://www.youtube.com/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_be_weakly_compatible () {
        /* Given */ YoutubeDownloaderCompatibility youtubeDownloaderCompatibility = new YoutubeDownloaderCompatibility();
        /* When  */ Integer compatibility = youtubeDownloaderCompatibility.compatibility("http://ma.video.fr/video.mp4");
        /* Then  */ assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_return_the_BeInSportUpdate_class () {
        /* Given */ YoutubeDownloaderCompatibility youtubeDownloaderCompatibility = new YoutubeDownloaderCompatibility();
        /* When  */ Class clazz = youtubeDownloaderCompatibility.downloader();
        /* Then  */ assertThat(clazz).isEqualTo(YoutubeDownloader.class);
    }

}