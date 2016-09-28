package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.URLInfo;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.factory.WGetFactory;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.Field;
import java.util.UUID;

import static lan.dk.podcastserver.manager.worker.downloader.ParleysDownloader.ParleysWatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 20/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class ParleysWatcherTest {

    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock ItemDownloadManager itemDownloadManager;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    @Mock FfmpegService ffmpegService;
    @Mock WGetFactory wGetFactory;
    @Mock JsonService jsonService;
    @Mock DownloadInfo info;
    ParleysDownloader parleysDownloader;
    Podcast podcast = Podcast.builder().id(UUID.randomUUID()).title("aPodcast").build();
    Item item;
    private ParleysWatcher watcher;

    @Before
    public void beforeEach() {
        parleysDownloader = new ParleysDownloader(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService, ffmpegService, wGetFactory, jsonService);

        item = Item.builder().url("http://aFakeUrl/foo.mp4").podcast(podcast).build();
        parleysDownloader.item = item;
        parleysDownloader.info = info;

        watcher = new ParleysWatcher(parleysDownloader);
        setTotalSize(parleysDownloader, 1000L);
    }

    private void setTotalSize(ParleysDownloader watcher, Long i) {
        try {
            Field f = ParleysDownloader.class.getDeclaredField("totalSize");
            f.setAccessible(true);
            f.set(watcher, i);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void should_handle_extracting() {
        /* Given */
        when(info.getState()).thenReturn(URLInfo.States.EXTRACTING);
        /* When */
        watcher.run();
    }
    
    @Test
    public void should_stop_when_error_happen() {
        /* Given */
        ParleysDownloader parleysDownloader = mock(ParleysDownloader.class);
        parleysDownloader.info = info;
        parleysDownloader.item = Item.builder().url("http://aFakeUrl/foo.mp4").title("anItem").podcast(Podcast.DEFAULT_PODCAST).build();
        ParleysWatcher watcher = new ParleysWatcher(parleysDownloader);
        when(info.getState()).thenReturn(URLInfo.States.ERROR);
        /* When */
        watcher.run();
        /* Then */
        verify(parleysDownloader, times(1)).stopDownload();
    }

    @Test
    public void should_increment_if_done() {
        /* Given */
        parleysDownloader.item.setProgression(50);
        when(info.getState())
                .thenReturn(URLInfo.States.DOWNLOADING)
                .thenReturn(URLInfo.States.DOWNLOADING)
                .thenReturn(URLInfo.States.DONE);
        when(info.getLength())
                .thenReturn(null)
                .thenReturn(0L);
        when(info.getCount()).thenReturn(900L);
        /* When */
        watcher.run();
        watcher.run();
        watcher.run();
        /* Then */
        assertThat(item.getProgression()).isEqualTo(90);
    }

    @Test
    public void should_do_nothing_on_retry() {
        /* Given */
        when(info.getState()).thenReturn(URLInfo.States.RETRYING);
        /* When */
        watcher.run();
    }

    @Test
    public void should_do_nothing_on_stop() {
        /* Given */
        when(info.getState()).thenReturn(URLInfo.States.STOP);
        /* When */
        watcher.run();
    }
}