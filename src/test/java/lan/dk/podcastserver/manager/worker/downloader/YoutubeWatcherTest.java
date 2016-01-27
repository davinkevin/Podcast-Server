package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadIOCodeError;
import lan.dk.podcastserver.entity.Item;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static lan.dk.podcastserver.manager.worker.downloader.YoutubeDownloader.YoutubeWatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 18/06/15 for HackerRank problem
 */
@RunWith(MockitoJUnitRunner.class)
public class YoutubeWatcherTest {

    @Captor ArgumentCaptor<Integer> progressionCaptore;

    @Mock VideoInfo info;
    @Mock DownloadInfo downloadInfo;
    @Mock VGet v;
    @Mock Item item;
    @Mock YoutubeDownloader YOUTUBE_DOWNLOADER;

    @Before
    public void beforeEach() {
        YOUTUBE_DOWNLOADER.info = info;
        YOUTUBE_DOWNLOADER.v = v;
        YOUTUBE_DOWNLOADER.item = item;

        when(info.getInfo()).thenReturn(downloadInfo);
    }

    @Test
    public void should_extract_information() {
        /* Given */
        YoutubeWatcher watcher = new YoutubeWatcher(YOUTUBE_DOWNLOADER);
        when(info.getState()).thenReturn(VideoInfo.States.EXTRACTING_DONE);
        /* When */
        watcher.run();
        /* Then */
        verify(info, times(1)).getInfo();
        verify(info, times(2)).getState();
        verify(item, times(1)).getUrl();
    }

    @Test
    public void should_stop_after_error() {
        /* Given */
        YoutubeWatcher watcher = new YoutubeWatcher(YOUTUBE_DOWNLOADER);
        when(info.getState()).thenReturn(VideoInfo.States.ERROR);
        /* When */
        watcher.run();
        /* Then */
        verify(YOUTUBE_DOWNLOADER, times(1)).stopDownload();
    }

    @Test
    public void should_finish_download() {
        /* Given */
        YoutubeWatcher watcher = new YoutubeWatcher(YOUTUBE_DOWNLOADER);
        when(info.getState()).thenReturn(VideoInfo.States.DONE);
        when(v.getTarget()).thenReturn(new File("/tmp/"));
        /* When */
        watcher.run();
        /* Then */
        verify(YOUTUBE_DOWNLOADER, times(1)).finishDownload();
    }

    @Test
    public void should_stop_download() {
        /* Given */
        YoutubeWatcher watcher = new YoutubeWatcher(YOUTUBE_DOWNLOADER);
        when(info.getState()).thenReturn(VideoInfo.States.STOP);
        /* When */
        watcher.run();
        verify(info, times(1)).getInfo();
        verify(info, times(1)).getState();
        verifyNoMoreInteractions(info, item,  v, YOUTUBE_DOWNLOADER);
    }

    @Test
    public void should_retry_and_stop_download() {
        /* Given */
        YoutubeWatcher watcher = new YoutubeWatcher(YOUTUBE_DOWNLOADER);
        watcher.MAX_WAITING_MINUTE = 0;
        when(info.getState()).thenReturn(VideoInfo.States.RETRYING);
        when(info.getDelay()).thenReturn(0);
        when(info.getException()).thenReturn(new DownloadIOCodeError(123));

        /* When */
        watcher.run();

        /* Then */
        verify(YOUTUBE_DOWNLOADER, times(1)).stopDownload();
    }

    @Test
    public void should_progress() {
        /* Given */
        YoutubeWatcher watcher = new YoutubeWatcher(YOUTUBE_DOWNLOADER);
        when(info.getState()).thenReturn(VideoInfo.States.DOWNLOADING);
        when(downloadInfo.getCount()).thenReturn(55L);
        when(downloadInfo.getLength()).thenReturn(100L);
        when(item.getProgression()).thenReturn(40);
        /* When */
        watcher.run();
        /* Then */
        verify(item, times(1)).setProgression(progressionCaptore.capture());
        assertThat(progressionCaptore.getValue()).isEqualTo(55);
        verify(YOUTUBE_DOWNLOADER, times(1)).convertAndSaveBroadcast();
    }
}