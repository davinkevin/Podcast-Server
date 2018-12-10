package lan.dk.podcastserver.manager.downloader;


import com.github.davinkevin.podcastserver.service.M3U8Service;
import com.github.davinkevin.podcastserver.service.MimeTypeService;
import com.github.davinkevin.podcastserver.service.ProcessService;
import com.github.davinkevin.podcastserver.service.UrlService;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import com.github.davinkevin.podcastserver.service.FfmpegService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import com.github.davinkevin.podcastserver.IOUtils;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static io.vavr.API.List;
import static io.vavr.API.Try;
import static java.util.concurrent.CompletableFuture.runAsync;
import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyVararg;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 20/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class FfmpegDownloaderTest {

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

    private @InjectMocks
    FfmpegDownloader ffmpegDownloader;

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
                .numberOfFail(0)
                .build();

        ffmpegDownloader.setItemDownloadManager(itemDownloadManager);
        ffmpegDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List(item.getUrl())).userAgent("Fake UserAgent").build());
        when(podcastServerParameters.getRootfolder()).thenReturn(IOUtils.ROOT_TEST_PATH);
        when(podcastServerParameters.getDownloadExtension()).thenReturn(".psdownload");
        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(podcast.getTitle()).toFile());
        Try(() -> Files.createDirectories(ROOT_TEST_PATH));
        ffmpegDownloader.postConstruct();
    }

    @Test
    public void should_download_file() {
        /* Given */
        when(ffmpegService.getDurationOf(anyString(), anyString())).thenReturn(1_000_000D);
        when(ffmpegService.download(anyString(), any(FFmpegBuilder.class), any(ProgressListener.class))).then(i -> {
            FFmpegBuilder builder = i.getArgument(1);
            String location = builder.build().stream().filter(s -> s.contains(ROOT_TEST_PATH.resolve(podcast.getTitle()).toString())).findFirst().orElseThrow(RuntimeException::new);
            Files.write(Paths.get(location), "".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING );
            Progress progress = new Progress();
            progress.out_time_ms = 90_000;
            ((ProgressListener) i.getArgument(2)).progress(progress);
            return mock(Process.class);
        });
        when(processService.waitFor(any())).thenReturn(new arrow.core.Try.Success<>(1));
        doAnswer(i -> {
            Path targetLocation = i.getArgument(0);
            Files.write(targetLocation, "".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return null;
        }).when(ffmpegService).concat(any(Path.class), anyVararg());

        /* When */
        Item downloaded = ffmpegDownloader.download();

        /* Then */
        assertThat(IOUtils.ROOT_TEST_PATH.resolve(podcast.getTitle()).resolve(item.getFileName())).exists();
        assertThat(downloaded).isSameAs(item);
        assertThat(downloaded.getStatus()).isSameAs(Status.FINISH);
        assertThat(item).hasProgression(9);
    }

    @Test
    public void should_be_compatible() {
        assertThat(ffmpegDownloader.compatibility(DownloadingItem.builder().urls(List(item.getUrl())).build())).isLessThan(Integer.MAX_VALUE/2);
        assertThat(ffmpegDownloader.compatibility(DownloadingItem.builder().urls(List("http://foo.bar/things.rss")).build())).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(ffmpegDownloader.compatibility(DownloadingItem.builder().urls(List("http://foo.bar/things.rss")).build())).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_restart_a_current_download() throws InterruptedException {
        /* Given */
        Process downloadProcess = mock(Process.class);
        item.setStatus(Status.PAUSED);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(downloadProcess);
        when(processService.start(any())).thenReturn(mock(Process.class));

        /* When */
        runAsync(() -> ffmpegDownloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(50));
        ffmpegDownloader.restartDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.STARTED);
        });
    }

    @Test
    public void should_failed_to_restart() {
        /* Given */
        Process downloadProcess = mock(Process.class);
        item.setStatus(Status.PAUSED);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(downloadProcess);
        doThrow(RuntimeException.class).when(processService).start(any());

        /* When */
        runAsync(() -> ffmpegDownloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(50));
        ffmpegDownloader.restartDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.FAILED);
        });
    }

    @Test
    public void should_paused_a_download() {
        /* Given */
        Process process = mock(Process.class);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(process);
        when(processService.waitFor(any())).then(i -> {
            TimeUnit.SECONDS.sleep(20L);
            return new arrow.core.Try.Success<>(10);
        });
        when(processService.start(any())).thenReturn(mock(Process.class));

        /* When */
        CompletableFuture<Void> future = runAsync(() -> ffmpegDownloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(50));
        ffmpegDownloader.pauseDownload();

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
        when(processService.waitFor(any())).then(i -> {
            TimeUnit.SECONDS.sleep(20L);
            return new arrow.core.Try.Success<>(1);
        });
        doThrow(RuntimeException.class).when(processService).start(any());

        /* When */
        CompletableFuture<Void> future = runAsync(() -> ffmpegDownloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(150));
        ffmpegDownloader.pauseDownload();

        /* Then */
        await().atMost(2, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.FAILED);
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
        CompletableFuture<Void> future = runAsync(() -> ffmpegDownloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(50));
        ffmpegDownloader.stopDownload();

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
        runAsync(() -> ffmpegDownloader.download());
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(500L));
        ffmpegDownloader.stopDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.STOPPED);
            verify(process).destroy();
        });
    }
    
    @Test
    public void should_return_target_if_already_set() {
        /* Given */
        ffmpegDownloader.target = Paths.get("/tmp/podcast/file.m3u8");
        /* When */
        Path targetFile = ffmpegDownloader.getTargetFile(Item.DEFAULT_ITEM);
        /* Then */
        assertThat(targetFile).isSameAs(ffmpegDownloader.target);
    }
}
