package lan.dk.podcastserver.manager.worker.downloader;

import com.google.common.collect.Lists;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.service.UrlService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jayway.awaitility.Awaitility.await;
import static lan.dk.podcastserver.manager.worker.downloader.M3U8Downloader.M3U8Watcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 23/01/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class M3U8WatcherTest {

    @Mock UrlService urlService;
    @Mock M3U8Downloader downloader;

    @Before
    public void beforeEach() throws IOException {
        downloader.target = Files.createTempFile(Paths.get("/tmp"), "tmp-", ".tmp").toFile();
        downloader.item = new Item().setUrl("www.url.in/format.m3u8").setStatus(Status.STARTED);
        downloader.urlService = urlService;
        downloader.urlList = Lists.newArrayList("www.url.in/format.1.m3u8", "www.url.in/format.2.m3u8", "www.url.in/format.3.m3u8");
        downloader.stopDownloading = new AtomicBoolean(false);
    }

    @Test
    public void should_download() throws IOException {
        /* Given */
        M3U8Watcher m3U8Watcher = new M3U8Watcher(downloader);
        InputStream is = mock(InputStream.class);

        when(urlService.asStream(startsWith("www.url.in"))).thenReturn(is);
        when(is.read(any())).thenReturn(-1);

        /* When */
        m3U8Watcher.run();

        /* Then */
        verify(downloader, times(1)).finishDownload();
    }
    
    @Test
    public void should_handle_error_on_input() throws IOException {
        /* Given */
        M3U8Watcher m3U8Watcher = new M3U8Watcher(downloader);
        doThrow(IOException.class).when(urlService).asStream(startsWith("www.url.in"));

        /* When */
        m3U8Watcher.run();

        /* Then */
        verify(downloader, times(1)).stopDownload();
    }

    @Test
    public void should_stop_download() throws IOException {
        /* Given */
        M3U8Watcher m3U8Watcher = new M3U8Watcher(downloader);
        InputStream is = mock(InputStream.class);

        when(urlService.asStream(startsWith("www.url.in"))).thenReturn(is);
        when(is.read(any())).thenReturn(-1);
        downloader.item.setStatus(Status.STOPPED);
        downloader.stopDownloading.set(true);

        /* When */
        m3U8Watcher.run();

        /* Then */
        verify(downloader, never()).finishDownload();
    }

    @Test
    public void should_pause_download() throws IOException, InterruptedException {
        /* Given */
        M3U8Watcher m3U8Watcher = new M3U8Watcher(downloader);
        InputStream is = mock(InputStream.class);

        when(urlService.asStream(startsWith("www.url.in"))).thenReturn(is);
        when(is.read(any())).thenReturn(-1);
        downloader.stopDownloading.set(true);
        downloader.item.setStatus(Status.PAUSED);

        /* When */
        Executors.newSingleThreadExecutor().submit(m3U8Watcher);
        relaunchWatcher(m3U8Watcher);

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(m3U8Watcher.hasBeenStarted().get()).isTrue();
            verify(downloader, times(1)).finishDownload();
        });
    }

    private void relaunchWatcher(M3U8Watcher m3U8Watcher) throws InterruptedException {
        TimeUnit.SECONDS.sleep(1L);
        downloader.stopDownloading.set(false);
        downloader.item.setStatus(Status.STARTED);
        synchronized (m3U8Watcher) {
            m3U8Watcher.notify();
        }
    }


}