package lan.dk.podcastserver.manager.worker.downloader;


import io.vavr.API;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.utils.IOUtils;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static io.vavr.API.*;
import static java.util.concurrent.CompletableFuture.runAsync;
import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static lan.dk.utils.IOUtils.ROOT_TEST_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 20/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class M3U8DownloaderTest {

    private @Mock PodcastRepository podcastRepository;
    private @Mock ItemRepository itemRepository;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SimpMessagingTemplate template;
    private @Mock MimeTypeService mimeTypeService;
    private @Mock ItemDownloadManager itemDownloadManager;

    private @Mock UrlService urlService;
    private @Mock M3U8Service m3U8Service;
    private @Mock FfmpegService ffmpegService;
    private @Mock ProcessService processService;

    private @InjectMocks M3U8Downloader m3U8Downloader;

    Podcast podcast;
    Item item;

    @Before
    public void beforeEach() {
        podcast = Podcast.builder()
                .title("M3U8Podcast")
                .build();

        item = Item.builder()
                .podcast(podcast)
                .url("http://foo.bar/com.m3u8")
                .status(Status.STARTED)
                .build();

        m3U8Downloader.setItemDownloadManager(itemDownloadManager);
        m3U8Downloader.setItem(item);
        when(podcastServerParameters.getRootfolder()).thenReturn(IOUtils.ROOT_TEST_PATH);
        when(podcastServerParameters.getDownloadExtension()).thenReturn(".psdownload");
        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(podcast.getTitle()).toFile());
        Try(() -> Files.createDirectories(ROOT_TEST_PATH));
        m3U8Downloader.postConstruct();
    }

    @Test
    public void should_download_file() throws IOException, URISyntaxException {
        /* Given */
        when(ffmpegService.getDurationOf(anyString(), anyString())).thenReturn(1_000_000D);
        when(ffmpegService.download(anyString(), any(FFmpegBuilder.class), any(ProgressListener.class))).then(i -> {
            FFmpegBuilder builder = i.getArgumentAt(1, FFmpegBuilder.class);
            String location = builder.build().stream().filter(s -> s.contains("/tmp/podcast-server-test/" + podcast.getTitle())).findFirst().orElseThrow(RuntimeException::new);
            Files.write(Paths.get(location), "".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING );
            Progress progress = new Progress();
            progress.out_time_ms = 90_000;
            i.getArgumentAt(2, ProgressListener.class).progress(progress);
            return mock(Process.class);
        });

        /* When */
        Item downloaded = m3U8Downloader.download();

        /* Then */
        assertThat(Paths.get("/tmp/podcast-server-test/", podcast.getTitle(), item.getFileName())).exists();
        assertThat(downloaded).isSameAs(item);
        assertThat(downloaded.getStatus()).isSameAs(Status.FINISH);
        assertThat(item).hasProgression(9);
    }

    @Test
    public void should_be_compatible() {
        assertThat(m3U8Downloader.compatibility(item.getUrl())).isLessThan(Integer.MAX_VALUE/2);
        assertThat(m3U8Downloader.compatibility("http://foo.bar/things.rss")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(m3U8Downloader.compatibility("http://foo.bar/things.rss")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_restart_a_current_download() throws IOException, URISyntaxException, InterruptedException {
        /* Given */
        Process downloadProcess = mock(Process.class);
        item.setStatus(Status.PAUSED);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(downloadProcess);
        when(downloadProcess.waitFor()).then(i -> {
            TimeUnit.SECONDS.sleep(10L);
            return 1;
        });
        when(processService.start(any())).then(i -> Try.run(() -> {}));

        /* When */
        runAsync(() -> m3U8Downloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(50));
        m3U8Downloader.restartDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.STARTED);
        });
    }

    @Test
    public void should_failed_to_restart() throws InterruptedException {
        /* Given */
        Process downloadProcess = mock(Process.class);
        item.setStatus(Status.PAUSED);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(downloadProcess);
        when(downloadProcess.waitFor()).then(i -> Try.run(() -> TimeUnit.SECONDS.sleep(10L)));
        when(processService.start(any())).then(i -> Try.failure(new IOException("Unable to restart download")));

        /* When */
        runAsync(() -> m3U8Downloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(50));
        m3U8Downloader.restartDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.STOPPED);
        });
    }

    @Test
    public void should_paused_a_download() {
        /* Given */
        Process process = mock(Process.class);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(process);
        when(processService.waitFor(any())).then(i -> Try.run(() -> TimeUnit.SECONDS.sleep(20L)));
        when(processService.start(any())).then(i -> Try.run(() -> {}));

        /* When */
        CompletableFuture<Void> future = runAsync(() -> m3U8Downloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(50));
        m3U8Downloader.pauseDownload();

        /* Then */
        await().atMost(2, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.PAUSED);
        });
        future.cancel(true);
    }
    
    @Test
    public void should_failed_to_pause() {
        /* Given */
        Process process = mock(Process.class);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(process);
        when(processService.waitFor(any())).then(i -> Try.run(() -> TimeUnit.SECONDS.sleep(20L)));
        when(processService.start(any())).then(i -> Try.failure(new IOException("Error during process")));

        /* When */
        CompletableFuture<Void> future = runAsync(() -> m3U8Downloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(50));
        m3U8Downloader.pauseDownload();

        /* Then */
        await().atMost(2, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.STOPPED);
        });
        future.cancel(true);
    }
    
    @Test
    public void should_stop_a_paused_download() throws IOException, URISyntaxException {
        /* Given */
        item.setStatus(Status.PAUSED);
        Process process = mock(Process.class);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(process);

        /* When */
        CompletableFuture<Void> future = runAsync(() -> m3U8Downloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(50));
        m3U8Downloader.stopDownload();

        /* Then */
        await().atMost(2, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.STOPPED);
        });
        future.cancel(true);
    }

    @Test
    public void should_stop_a_current_download() throws IOException, URISyntaxException {
        /* Given */
        Process process = mock(Process.class);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(process);

        /* When */
        runAsync(() -> m3U8Downloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(500L));
        m3U8Downloader.stopDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.STOPPED);
            verify(process).destroy();
        });
    }
    
    @Test
    public void should_return_target_if_already_set() {
        /* Given */
        m3U8Downloader.target = Paths.get("/tmp/podcast/file.m3u8");
        /* When */
        Path targetFile = m3U8Downloader.getTargetFile(Item.DEFAULT_ITEM);
        /* Then */
        assertThat(targetFile).isSameAs(m3U8Downloader.target);
    }
}
