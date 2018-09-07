package lan.dk.podcastserver.manager.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static lan.dk.podcastserver.manager.downloader.RTMPDownloader.RTMPWatcher;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 27/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class RTMPWatcherTest {

    private static final InputStream LOG = new ByteArrayInputStream(new StringBuilder()
            .append("Progression : (1%)\n")
            .append("Progression : (2%)\n")
            .append("Progression : (3%)\n")
            .append("Download Complete\n")
            .toString()
            .getBytes()
    );

    private static final InputStream ERROR_LOG = new ByteArrayInputStream(new StringBuilder()
            .append("Error...\n")
            .append("Error...\n")
            .append("Error...\n")
            .toString()
            .getBytes()
    );

    @Mock Process process;
    @Mock RTMPDownloader rtmpDownloader;
    private RTMPWatcher rtmpWatcher;

    @Before
    public void beforeEach() {
        rtmpDownloader.p = process;
        rtmpDownloader.item = Item
                .builder()
                    .status(Status.STARTED)
                    .progression(0)
                .build();

        rtmpDownloader.pid = 1234;
        rtmpDownloader.stopDownloading = new AtomicBoolean(true);

        rtmpWatcher = new RTMPWatcher(rtmpDownloader);
    }

    @Test
    public void should_extract_progression_from_log() {
        /* Given */
        when(process.getInputStream()).thenReturn(LOG);
        doAnswer( i -> rtmpDownloader.item.setStatus(Status.FINISH)).when(rtmpDownloader).finishDownload();

        /* When */
        rtmpWatcher.run();

        /* Then */
        verify(rtmpDownloader, times(3)).convertAndSaveBroadcast();
        verify(rtmpDownloader, times(1)).finishDownload();
        assertThat(rtmpDownloader.item)
                .hasProgression(3)
                .hasStatus(Status.FINISH);
    }
    
    @Test
    public void should_exit_if_no_pid() {
        /* Given */
        when(process.getInputStream()).thenReturn(ERROR_LOG);
        rtmpDownloader.pid = 0;
        rtmpDownloader.stopDownloading = new AtomicBoolean(false);

        /* When */
        assertThatThrownBy(() -> rtmpWatcher.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessageStartingWith("Unexpected ending, failed download");

        /* Then */
        verify(rtmpDownloader, never()).convertAndSaveBroadcast();
        verify(rtmpDownloader, never()).finishDownload();
    }
    
    @Test
    public void should_reset_if_error_in_log() throws IOException {
        /* Given */
        InputStream errorStream = mock(InputStream.class);
        when(process.getInputStream()).thenReturn(errorStream);

        rtmpDownloader.pid = 1234;
        rtmpDownloader.stopDownloading = new AtomicBoolean(false);

        /* When */
        assertThatThrownBy(() -> rtmpWatcher.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessageStartingWith("Unexpected ending, failed download");

        /* Then */
        verify(rtmpDownloader, never()).convertAndSaveBroadcast();
        verify(rtmpDownloader, never()).finishDownload();
    }
}
