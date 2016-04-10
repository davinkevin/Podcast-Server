package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.ItemAssert;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.UrlService;
import org.apache.commons.io.input.NullInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 20/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class M3U8DownloaderTest {

    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock ItemDownloadManager itemDownloadManager;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    @Mock UrlService urlService;
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
        when(itemDownloadManager.getRootfolder()).thenReturn("/tmp");
        when(urlService.getFileNameM3U8Url(any())).thenCallRealMethod();
        when(urlService.urlWithDomain(any(), any())).thenCallRealMethod();
        when(podcastServerParameters.getDownloadExtension()).thenReturn(".psdownload");
        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        m3U8Downloader.postConstruct();
    }

    @Test
    public void should_wake_up_downloader_watcher_if_already_working() {
        /* Given */
        m3U8Downloader.watcher.hasBeenStarted().set(true);

        /* When */
        Item downloadedItem = m3U8Downloader.download();

        /* Then */
        assertThat(downloadedItem).isEqualTo(item);
        ItemAssert
                .assertThat(downloadedItem)
                .hasStatus(Status.STARTED);
    }

    @Test
    public void should_load_each_url_of_m3u8_file() throws IOException, URISyntaxException {
        /* Given */
        when(urlService.urlAsReader(eq(item.getUrl()))).thenReturn(filesFrom("/remote/downloader/m3u8/m3u8file.m3u8"));
        when(urlService.asStream(any())).thenReturn(new NullInputStream(1000));

        /* When */
        Item downloaded = m3U8Downloader.download();

        /* Then */
        assertThat(Paths.get("/tmp", podcast.getTitle(), item.getFileName())).exists();
        assertThat(downloaded).isSameAs(item);
        assertThat(downloaded.getStatus()).isSameAs(Status.FINISH);
        verify(urlService, times(5)).asStream(any());
    }

    @Test
    public void should_stop_download_if_error_in_reader_of_m3u8() throws IOException {
        /* Given */
        when(urlService.urlAsReader(item.getUrl())).thenThrow(new IOException());

        /* When */
        Item downloaded = m3U8Downloader.download();

        /* Then */
        assertThat(downloaded).isSameAs(item);
        assertThat(downloaded.getStatus()).isSameAs(Status.STOPPED);
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

    private BufferedReader filesFrom(String s) throws URISyntaxException, IOException {
        return Files.newBufferedReader(Paths.get(M3U8DownloaderTest.class.getResource(s).toURI()));
    }

}