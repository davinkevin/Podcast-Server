package lan.dk.podcastserver.manager.downloader;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
public class NoOpDownloaderTest {

    @Test
    public void should_return_default_value() {
         /* Given */
        NoOpDownloader noOpDownloader = new NoOpDownloader();

        /* When */
        noOpDownloader.startDownload();
        noOpDownloader.pauseDownload();
        noOpDownloader.restartDownload();
        noOpDownloader.stopDownload();
        noOpDownloader.finishDownload();
        noOpDownloader.failDownload();
        noOpDownloader.run();

        /* Then */
        assertThat(noOpDownloader.download()).isNull();
        assertThat(noOpDownloader.setDownloadingItem(null)).isSameAs(noOpDownloader);
        assertThat(noOpDownloader.getItem()).isNull();
        assertThat(noOpDownloader.getItemUrl(null)).isNull();
        assertThat(noOpDownloader.compatibility(null)).isEqualTo(-1);
        assertThat(noOpDownloader.setItemDownloadManager(null)).isEqualTo(noOpDownloader);
    }
}
