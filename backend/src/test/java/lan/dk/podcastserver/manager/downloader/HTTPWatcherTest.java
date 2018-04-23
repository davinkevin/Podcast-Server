package lan.dk.podcastserver.manager.downloader;

import com.github.axet.wget.info.DownloadInfo;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.axet.wget.info.URLInfo.States.*;
import static lan.dk.podcastserver.manager.downloader.HTTPDownloader.HTTPWatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 24/01/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class HTTPWatcherTest {

    @Mock DownloadInfo info;
    @Mock ItemDownloadManager itemDownloadManager;
    @Mock HTTPDownloader downloader;
    HTTPWatcher httpWatcher;


    @Before
    public void beforeEach() {
        downloader.item = new Item().setUrl("http://a.fake.url/with/a/file.mp4").setProgression(0);
        downloader.itemDownloadManager = itemDownloadManager;
        downloader.info = info;

        when(downloader.getItemUrl(downloader.item)).thenReturn(downloader.item.getUrl());

        httpWatcher = new HTTPWatcher(downloader);
    }

    @Test
    public void should_do_extraction() {
        /* Given */
        when(info.getState()).thenReturn(EXTRACTING);

        /* When */
        httpWatcher.run();

        /* Then */
        verify(downloader, never()).convertAndSaveBroadcast();
        verify(downloader, never()).stopDownload();
        verify(downloader, never()).finishDownload();
        verify(itemDownloadManager, never()).removeACurrentDownload(any());
    }

    @Test
    public void should_do_extraction_done() {
        /* Given */
        when(info.getState()).thenReturn(EXTRACTING_DONE);

        /* When */
        httpWatcher.run();

        /* Then */
        verify(downloader, never()).convertAndSaveBroadcast();
        verify(downloader, never()).stopDownload();
        verify(itemDownloadManager, never()).removeACurrentDownload(any());
        verify(downloader, never()).finishDownload();
    }

    @Test
    public void should_do_done() {
        /* Given */
        when(info.getState()).thenReturn(DONE);

        /* When */
        httpWatcher.run();

        /* Then */
        verify(downloader, times(1)).finishDownload();
        verify(itemDownloadManager, times(1)).removeACurrentDownload(any());
        verify(downloader, never()).stopDownload();
        verify(downloader, never()).convertAndSaveBroadcast();
    }

    @Test
    public void should_do_retrying() {
        /* Given */
        when(info.getState()).thenReturn(RETRYING);

        /* When */
        httpWatcher.run();

        /* Then */
        verify(downloader, never()).stopDownload();
        verify(downloader, never()).convertAndSaveBroadcast();
        verify(itemDownloadManager, never()).removeACurrentDownload(any());
        verify(downloader, never()).finishDownload();
    }

    @Test
    public void should_do_stop() {
        /* Given */
        when(info.getState()).thenReturn(STOP);

        /* When */
        httpWatcher.run();

        /* Then */
        verify(downloader, never()).finishDownload();
        verify(downloader, never()).convertAndSaveBroadcast();
        verify(downloader, never()).stopDownload();
        verify(itemDownloadManager, never()).removeACurrentDownload(any());
    }

    @Test
    public void should_do_downloading_without_length() {
        /* Given */
        when(info.getState()).thenReturn(DOWNLOADING);

        /* When */
        httpWatcher.run();

        /* Then */
        verify(downloader, never()).finishDownload();
        verify(downloader, never()).convertAndSaveBroadcast();
        verify(itemDownloadManager, never()).removeACurrentDownload(any());
        verify(downloader, never()).stopDownload();
    }
    
    @Test
    public void should_do_downloading_with_length() {
        /* Given */
        when(info.getState()).thenReturn(DOWNLOADING);
        when(info.getCount()).thenReturn(5L);
        when(info.getLength()).thenReturn(10L);

        /* When */
        httpWatcher.run();

        /* Then */
        assertThat(downloader.item.getProgression()).isEqualTo(50);
        verify(downloader, never()).finishDownload();
        verify(downloader, never()).stopDownload();
        verify(itemDownloadManager, never()).removeACurrentDownload(any());
        verify(downloader, times(1)).convertAndSaveBroadcast();
    }

}
