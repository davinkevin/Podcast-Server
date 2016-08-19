package lan.dk.podcastserver.manager.worker.downloader;

import javaslang.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.FfmpegService;
import lan.dk.podcastserver.service.M3U8Service;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.factory.ProcessBuilderFactory;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.CompletableFuture.runAsync;
import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 20/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class M3U8DownloaderTest {

    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    @Mock ItemDownloadManager itemDownloadManager;

    @Mock
    UrlService urlService;
    @Mock M3U8Service m3U8Service;
    @Mock FfmpegService ffmpegService;
    @Mock ProcessBuilderFactory processBuilderFactory;

    @InjectMocks M3U8Downloader m3U8Downloader;

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
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/tmp"));
        when(podcastServerParameters.getDownloadExtension()).thenReturn(".psdownload");
        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        m3U8Downloader.postConstruct();
    }

    @Test
    public void should_load_each_url_of_m3u8_file() throws IOException, URISyntaxException {
        /* Given */
        when(ffmpegService.getDurationOf(anyString(), anyString())).thenReturn(1000D);
        when(ffmpegService.download(anyString(), any(FFmpegBuilder.class), any())).then(i -> {
            FFmpegBuilder builder = i.getArgumentAt(1, FFmpegBuilder.class);
            String location = builder.build().stream().filter(s -> s.contains("/tmp/" + podcast.getTitle())).findFirst().orElseThrow(RuntimeException::new);
            Files.write(Paths.get(location), "".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING );
            return mock(Process.class);
        });

        /* When */
        Item downloaded = m3U8Downloader.download();

        /* Then */
        assertThat(Paths.get("/tmp", podcast.getTitle(), item.getFileName())).exists();
        assertThat(downloaded).isSameAs(item);
        assertThat(downloaded.getStatus()).isSameAs(Status.FINISH);
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
        ProcessBuilder stopProcess = new ProcessBuilder("echo", "foo");
        Process downloadProcess = mock(Process.class);
        item.setStatus(Status.PAUSED);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(downloadProcess);
        when(downloadProcess.waitFor()).then(i -> {
            TimeUnit.SECONDS.sleep(10L);
            return 1;
        });
        when(processBuilderFactory.newProcessBuilder(anyVararg())).then(i -> stopProcess);
        /* When */
        runAsync(() -> m3U8Downloader.download());
        Try.run(() -> TimeUnit.SECONDS.sleep(1));
        m3U8Downloader.restartDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.STARTED);
        });
    }

    @Test
    public void should_stop_a_paused_download() throws IOException, URISyntaxException {
        /* Given */
        item.setStatus(Status.PAUSED);
        Process process = mock(Process.class);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(process);

        /* When */
        runAsync(() -> m3U8Downloader.download());
        Try.run(() -> TimeUnit.SECONDS.sleep(3));
        m3U8Downloader.stopDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(item).hasStatus(Status.STOPPED);
        });
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
        });
    }
}