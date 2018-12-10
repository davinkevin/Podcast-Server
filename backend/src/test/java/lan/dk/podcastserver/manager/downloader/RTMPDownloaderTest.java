package lan.dk.podcastserver.manager.downloader;

import com.github.davinkevin.podcastserver.service.MimeTypeService;
import com.github.davinkevin.podcastserver.service.ProcessService;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.properties.ExternalTools;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import static io.vavr.API.List;
import static io.vavr.API.Try;
import static com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH;
import static com.github.davinkevin.podcastserver.IOUtils.TEMPORARY_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 27/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class RTMPDownloaderTest {

    private @Mock ExternalTools externalTools;
    private @Mock PodcastRepository podcastRepository;
    private @Mock ItemRepository itemRepository;
    private @Mock ItemDownloadManager itemDownloadManager;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SimpMessagingTemplate template;
    private @Mock MimeTypeService mimeTypeService;
    private @Mock ProcessService processService;
    private @InjectMocks RTMPDownloader rtmpDownloader;

    private @Captor ArgumentCaptor<String> processParameters;

    private Item item;

    @Before
    public void beforeEach() {
        Podcast podcast = Podcast.builder()
                .id(UUID.randomUUID())
                .title("RTMP Podcast")
                .build();
        item = Item
                .builder()
                .url("rtmp://a.url.com/foo/bar.mp4")
                .status(Status.STARTED)
                .podcast(podcast)
                .progression(0)
                .numberOfFail(0)
                .build();
        when(podcastServerParameters.getDownloadExtension()).thenReturn(TEMPORARY_EXTENSION);
        when(externalTools.getRtmpdump()).thenReturn("/usr/local/bin/rtmpdump");
        when(podcastServerParameters.getRootfolder()).thenReturn(ROOT_TEST_PATH);
        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));

        rtmpDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List()).build());
        rtmpDownloader.setItemDownloadManager(itemDownloadManager);

        rtmpDownloader.postConstruct();
        FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(podcast.getTitle()).toFile());
        Try(() -> Files.createDirectories(ROOT_TEST_PATH));
    }

    @Test
    public void should_be_a_downloader() {
        assertThat(Downloader.class.isInstance(rtmpDownloader));
        assertThat(AbstractDownloader.class.isInstance(rtmpDownloader));
    }

    @Test
    public void should_download() throws IOException, URISyntaxException {
        /* Given */
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/cat", IOUtils.toPath("/remote/downloader/rtmpdump/rtmpdump.txt").get().toString());
        when(processService.newProcessBuilder((String[]) anyVararg())).then(i -> {
            Files.createFile(ROOT_TEST_PATH.resolve(item.getPodcast().getTitle()).resolve("bar.mp4" + TEMPORARY_EXTENSION));
            return processBuilder;
        });
        when(processService.pidOf(any())).thenReturn(1234);

        /* When */
        rtmpDownloader.download();

        /* Then */
        assertThat(rtmpDownloader.pid).isEqualTo(0);
        assertThat(ROOT_TEST_PATH.resolve(item.getPodcast().getTitle()).resolve("bar.mp4")).exists();
        verify(processService, times(1)).newProcessBuilder(processParameters.capture());
        assertThat(processParameters.getAllValues())
                .hasSize(5)
                .containsExactly(
                        "/usr/local/bin/rtmpdump",
                        "-r",
                        item.getUrl(),
                        "-o",
                        ROOT_TEST_PATH.resolve(item.getPodcast().getTitle()).resolve("bar.mp4" + TEMPORARY_EXTENSION).toFile().getAbsolutePath()
                );

    }

    @Test
    public void should_stop_download_if_ioexception() throws URISyntaxException {
        /* Given */
        ProcessBuilder processBuilder = new ProcessBuilder("/bin");
        when(processService.newProcessBuilder((String[]) anyVararg())).then(i -> processBuilder);
        rtmpDownloader.p = mock(Process.class);

        /* When */
        assertThatThrownBy(() -> rtmpDownloader.download())

        /* Then */
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void should_stop_process_if_failed() {
        /* GIVEN */
        ProcessBuilder processBuilder = new ProcessBuilder("/bin");
        rtmpDownloader.p = mock(Process.class);
        /* WHEN  */
        rtmpDownloader.failDownload();
        /* THEN  */
        verify(rtmpDownloader.p, times(1)).destroy();
        assertThat(rtmpDownloader.item.getStatus()).isEqualByComparingTo(Status.FAILED);
    }

    @Test
    public void should_destroy_previous_process_at_start() throws URISyntaxException {
        /* Given */
        Process process = mock(Process.class);
        rtmpDownloader.pid = 123;
        rtmpDownloader.p = process;

        ProcessBuilder processBuilder = new ProcessBuilder("/bin/cat", IOUtils.toPath("/remote/downloader/rtmpdump/rtmpdump.txt").toString());
        when(processService.newProcessBuilder((String[]) anyVararg())).then(i -> {
            Files.createFile(ROOT_TEST_PATH.resolve(item.getPodcast().getTitle()).resolve("bar.mp4" + TEMPORARY_EXTENSION));
            return processBuilder;
        });
        when(processService.pidOf(any())).thenReturn(1234);

        /* When */
        rtmpDownloader.startDownload();

        /* Then */
        verify(process, times(1)).destroy();
    }

    @Test
    public void should_pause_process_on_pause_of_download() {
        /* Given */
        rtmpDownloader.pid = 123;
        when(processService.newProcessBuilder((String[]) anyVararg())).thenReturn(new ProcessBuilder("/bin/ls"));

        /* When */
        rtmpDownloader.pauseDownload();

        /* Then */
        verify(processService, times(1)).newProcessBuilder(processParameters.capture());
        assertThat(processParameters.getAllValues())
                .hasSize(3)
                .containsExactly(
                        "kill",
                        "-STOP",
                        "123"
                );
    }

    @Test
    public void should_stop_if_pause_not_working() {
        /* Given */
        Process process = mock(Process.class);
        rtmpDownloader.pid = 123;
        rtmpDownloader.p = process;
        when(processService.newProcessBuilder((String[]) anyVararg())).thenReturn(new ProcessBuilder("/bin"));

        /* When */
        rtmpDownloader.pauseDownload();

        /* Then */
        verify(process, times(1)).destroy();
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
    }

    @Test
    public void should_be_compatible() {
        assertThat(rtmpDownloader.compatibility(DownloadingItem.builder().urls(List("http://foo.bar.com/video")).build())).isGreaterThan(1);
        assertThat(rtmpDownloader.compatibility(DownloadingItem.builder().urls(List("rtmp://foo.bar.com/video")).build())).isEqualTo(1);
    }
}
