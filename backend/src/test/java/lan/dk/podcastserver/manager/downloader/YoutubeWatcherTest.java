package lan.dk.podcastserver.manager.downloader;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadIOCodeError;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.CompletableFuture.runAsync;
import static lan.dk.podcastserver.manager.downloader.YoutubeDownloader.YoutubeWatcher;
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
        YOUTUBE_DOWNLOADER.v = v;
        YOUTUBE_DOWNLOADER.item = item;

        when(v.getVideo()).thenReturn(info);
        when(info.getInfo()).thenReturn(generate(2));
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
        verify(YOUTUBE_DOWNLOADER, times(1)).failDownload();
    }

    @Test
    public void should_finish_download() {
        /* Given */
        YoutubeWatcher watcher = new YoutubeWatcher(YOUTUBE_DOWNLOADER);
        when(info.getState()).thenReturn(VideoInfo.States.DONE);
        when(info.getInfo()).thenReturn(generate(2));
        when(item.getStatus()).thenReturn(Status.STARTED);
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
        verify(item, times(1)).getStatus();
    }

    @Test
    public void should_retry_and_stop_download() {
        /* Given */
        YoutubeWatcher watcher = new YoutubeWatcher(YOUTUBE_DOWNLOADER);
        watcher.MAX_WAITING_MINUTE = -1;
        when(info.getState()).thenReturn(VideoInfo.States.RETRYING);
        when(info.getDelay()).thenReturn(0);
        when(info.getException()).thenReturn(new DownloadIOCodeError(123));

        /* When */
        watcher.run();

        /* Then */
        verify(YOUTUBE_DOWNLOADER, times(1)).failDownload();
    }

    @Test
    public void should_progress() {
        /* Given */
        YoutubeWatcher watcher = new YoutubeWatcher(YOUTUBE_DOWNLOADER);
        when(info.getInfo()).thenReturn(generate(1));
        when(info.getState()).thenReturn(VideoInfo.States.DOWNLOADING);
        when(item.getProgression()).thenReturn(40);
        /* When */
        watcher.run();
        /* Then */
        verify(item, times(1)).setProgression(eq(56));
        verify(YOUTUBE_DOWNLOADER, times(1)).convertAndSaveBroadcast();
    }

    @Test
    public void should_wait_in_pause_status_of_item() {
        /* Given */
        final YoutubeWatcher watcher = new YoutubeWatcher(YOUTUBE_DOWNLOADER);
        when(info.getInfo()).thenReturn(generate(1));
        when(info.getState()).thenReturn(VideoInfo.States.ERROR);
        when(item.getStatus()).thenReturn(Status.PAUSED);

        /* When */
        runAsync(watcher);
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(100));
        synchronized (watcher) { watcher.notify(); }

        /* Then */
        await().atMost(1, TimeUnit.SECONDS).until(() -> {
            verify(YOUTUBE_DOWNLOADER, atLeast(1)).failDownload();
        });
    }

    private List<VideoFileInfo> generate(Integer number) {
        return IntStream
                .range(0, number)
                .mapToObj(i -> {
                    VideoFileInfo v = new VideoFileInfo(null);
                    v.targetFile =  new File("/tmp/foo" + i);
                    v.setContentType( i == 0 ? "video/mp4" : "audio/webm");
                    v.setLength(1000L);
                    v.setCount(560L);
                    return v;
                }).collect(Collectors.toList());

    }
}
