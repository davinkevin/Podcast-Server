package lan.dk.podcastserver.manager.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
public class NoOpDownloaderTest {

    @Test
    public void should_return_default_value() {
         /* Given */
        NoOpDownloader noOpDownloader = new NoOpDownloader();

        /* When */
        noOpDownloader.pauseDownload();
        noOpDownloader.restartDownload();
        noOpDownloader.stopDownload();
        noOpDownloader.finishDownload();

        /* Then */
        assertThat(noOpDownloader.download()).isNull();
        assertThat(noOpDownloader.setDownloadingItem(null)).isSameAs(noOpDownloader);
        assertThat(noOpDownloader.getItem()).isNull();
        assertThat(noOpDownloader.getItemUrl(null)).isNull();
        assertThat(noOpDownloader.compatibility(null)).isEqualTo(-1);
        assertThat(noOpDownloader.setItemDownloadManager(null)).isEqualTo(noOpDownloader);
    }

    @Test
    public void should_remove_itself_from_idm_when_start() {
        /* GIVEN */
        ItemDownloadManager idm = mock(ItemDownloadManager.class);
        DownloadingItem di = DownloadingItem.builder()
                .item(Item.DEFAULT_ITEM)
                .build();
        NoOpDownloader noOpDownloader = new NoOpDownloader()
                .setDownloadingItem(di)
                .setItemDownloadManager(idm);

        /* WHEN  */
        noOpDownloader.run();

        /* THEN  */
        verify(idm, times(1)).removeACurrentDownload(Item.DEFAULT_ITEM);
    }
}
