package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.ProcessService;
import lan.dk.podcastserver.service.properties.ExternalTools;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
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
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 27/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class RTMPDownloaderTest {

    @Mock ExternalTools externalTools;
    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock ItemDownloadManager itemDownloadManager;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    @Mock ProcessService processService;
    @InjectMocks RTMPDownloader rtmpDownloader;

    @Captor ArgumentCaptor<String> processParameters;

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
                .build();
        when(podcastServerParameters.getDownloadExtension()).thenReturn(".psdownload");
        when(externalTools.getRtmpdump()).thenReturn("/usr/local/bin/rtmpdump");
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/tmp"));
        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);

        rtmpDownloader.setItem(item);
        rtmpDownloader.setItemDownloadManager(itemDownloadManager);

        rtmpDownloader.postConstruct();
        FileSystemUtils.deleteRecursively(Paths.get("/tmp", podcast.getTitle()).toFile());
    }

    @Test
    public void should_be_a_downloader() {
        assertThat(Downloader.class.isInstance(rtmpDownloader));
        assertThat(AbstractDownloader.class.isInstance(rtmpDownloader));
    }

    @Test
    public void should_download() throws IOException, URISyntaxException {
        /* Given */
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/cat", fileUri("/remote/downloader/rtmpdump/rtmpdump.txt"));
        when(processService.newProcessBuilder((String[]) anyVararg())).then(i -> {
            Files.createFile(Paths.get("/tmp", "RTMP Podcast", "bar.mp4.psdownload"));
            return processBuilder;
        });
        when(processService.pidOf(any())).thenReturn(1234);

        /* When */
        rtmpDownloader.download();

        /* Then */
        assertThat(rtmpDownloader.pid).isEqualTo(0);
        assertThat(Paths.get("/tmp", "RTMP Podcast", "bar.mp4")).exists();
        verify(processService, times(1)).newProcessBuilder(processParameters.capture());
        assertThat(processParameters.getAllValues())
                .hasSize(5)
                .containsExactly(
                        "/usr/local/bin/rtmpdump",
                        "-r",
                        item.getUrl(),
                        "-o",
                        Paths.get("/tmp", "RTMP Podcast", "bar.mp4.psdownload").toFile().getAbsolutePath()
                );

    }

    @Test
    public void should_stop_download_if_ioexception() throws URISyntaxException {
        /* Given */
        ProcessBuilder processBuilder = new ProcessBuilder("/bin");
        when(processService.newProcessBuilder((String[]) anyVararg())).then(i -> processBuilder);
        when(processService.pidOf(any())).thenReturn(1234);
        rtmpDownloader.p = mock(Process.class);

        /* When */
        rtmpDownloader.download();

        /* Then */
        verify(rtmpDownloader.p, times(1)).destroy();
    }

    @Test
    public void should_destroy_previous_process_at_start() throws URISyntaxException {
        /* Given */
        Process process = mock(Process.class);
        rtmpDownloader.pid = 123;
        rtmpDownloader.p = process;

        ProcessBuilder processBuilder = new ProcessBuilder("/bin/cat", fileUri("/remote/downloader/rtmpdump/rtmpdump.txt"));
        when(processService.newProcessBuilder((String[]) anyVararg())).then(i -> {
            Files.createFile(Paths.get("/tmp", "RTMP Podcast", "bar.mp4.psdownload"));
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
        assertThat(rtmpDownloader.compatibility("http://foo.bar.com/video")).isGreaterThan(1);
        assertThat(rtmpDownloader.compatibility("rtmp://foo.bar.com/video")).isEqualTo(1);
    }

    private String fileUri(String relativePath) throws URISyntaxException {
        return Paths.get(RTMPDownloaderTest.class.getResource(relativePath).toURI()).toString();
    }
}
