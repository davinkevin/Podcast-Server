package lan.dk.podcastserver.manager.downloader;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import io.vavr.collection.HashSet;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import com.github.davinkevin.podcastserver.service.UrlService;
import com.github.davinkevin.podcastserver.service.factory.WGetFactory;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.vavr.API.List;
import static io.vavr.API.Try;
import static lan.dk.podcastserver.manager.downloader.HTTPDownloader.HTTPWatcher;
import static lan.dk.podcastserver.manager.downloader.HTTPDownloader.WS_TOPIC_DOWNLOAD;
import static com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH;
import static com.github.davinkevin.podcastserver.IOUtils.TEMPORARY_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 30/01/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class HTTPDownloaderTest {

    private @Mock ItemRepository itemRepository;
    private @Mock PodcastRepository podcastRepository;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SimpMessagingTemplate template;
    // @Mock MimeTypeService mimeTypeService;
    private @Mock WGetFactory wGetFactory;
    private @Mock UrlService urlService;
    private @Mock ItemDownloadManager itemDownloadManager;
    private @InjectMocks HTTPDownloader httpDownloader;

    Podcast podcast;
    Item item;

    @Before
    public void beforeEach() {
        item = Item.builder()
                    .title("Title")
                    .url("http://a.fake.url/with/file.mp4?param=1")
                    .status(Status.NOT_DOWNLOADED)
                    .numberOfFail(0)
                .build();
        podcast = Podcast.builder()
                .id(UUID.randomUUID())
                .title("A Fake Http Podcast")
                .items(HashSet.<Item>empty().toJavaSet())
                .build()
                .add(item);

        httpDownloader.setItemDownloadManager(itemDownloadManager);
        when(podcastServerParameters.getDownloadExtension()).thenReturn(TEMPORARY_EXTENSION);
        httpDownloader.postConstruct();
        when(podcastServerParameters.getRootfolder()).thenReturn(IOUtils.ROOT_TEST_PATH);
        FileSystemUtils.deleteRecursively(IOUtils.ROOT_TEST_PATH.resolve(podcast.getTitle()).toFile());

        FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(podcast.getTitle()).toFile());
        Try(() -> Files.createDirectories(ROOT_TEST_PATH));
    }

    @Test
    public void should_run_download() throws MalformedURLException {
        /* Given */
        httpDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List(item.getUrl())).build());

        DownloadInfo downloadInfo = mock(DownloadInfo.class);
        WGet wGet = mock(WGet.class);

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(urlService.getRealURL(anyString())).then(i -> i.getArguments()[0]);
        when(wGetFactory.newDownloadInfo(anyString())).thenReturn(downloadInfo);
        when(wGetFactory.newWGet(any(DownloadInfo.class), any(File.class))).thenReturn(wGet);
        doAnswer(i -> {
            Files.createFile(IOUtils.ROOT_TEST_PATH.resolve(podcast.getTitle()).resolve("file.mp4" + TEMPORARY_EXTENSION));
            item.setStatus(Status.FINISH);
            httpDownloader.finishDownload();
            return null;
        }).when(wGet).download(any(AtomicBoolean.class), any(HTTPWatcher.class));

        /* When */
        httpDownloader.run();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FINISH);
        verify(podcastRepository, atLeast(1)).findById(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        assertThat(httpDownloader.target).isEqualTo(IOUtils.ROOT_TEST_PATH.resolve("A Fake Http Podcast").resolve("file.mp4"));
    }

    @Test
    public void should_stop_download() throws MalformedURLException {
        /* Given */
        httpDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List(item.getUrl())).build());

        DownloadInfo downloadInfo = mock(DownloadInfo.class);
        WGet wGet = mock(WGet.class);

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(urlService.getRealURL(anyString())).then(i -> i.getArguments()[0]);
        when(wGetFactory.newDownloadInfo(anyString())).thenReturn(downloadInfo);
        when(wGetFactory.newWGet(any(DownloadInfo.class), any(File.class))).thenReturn(wGet);
        doAnswer(i -> Files.createFile(IOUtils.ROOT_TEST_PATH.resolve(podcast.getTitle()).resolve( "file.mp4" + TEMPORARY_EXTENSION)))
                .when(wGet).download(any(AtomicBoolean.class), any(HTTPWatcher.class));

        /* When */
        httpDownloader.run();
        httpDownloader.stopDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
        verify(podcastRepository, atLeast(1)).findById(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        assertThat(httpDownloader.target).isEqualTo(IOUtils.ROOT_TEST_PATH.resolve("A Fake Http Podcast").resolve("file.mp4" + TEMPORARY_EXTENSION));
    }

    @Test
    public void should_handle_multipart_download_error() throws MalformedURLException {
        /* Given */
        httpDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List(item.getUrl())).build());

        DownloadInfo downloadInfo = mock(DownloadInfo.class);
        WGet wGet = mock(WGet.class);
        DownloadMultipartError error = mock(DownloadMultipartError.class);

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(urlService.getRealURL(anyString())).then(i -> i.getArguments()[0]);
        when(wGetFactory.newDownloadInfo(anyString())).thenReturn(downloadInfo);
        when(wGetFactory.newWGet(any(DownloadInfo.class), any(File.class))).thenReturn(wGet);
        when(error.getInfo()).thenReturn(downloadInfo);
        when(downloadInfo.getParts()).thenReturn(List(mock(DownloadInfo.Part.class), mock(DownloadInfo.Part.class), mock(DownloadInfo.Part.class)).toJavaList());
        doThrow(error).when(wGet).download(any(AtomicBoolean.class), any(HTTPWatcher.class));

        /* When */
        httpDownloader.run();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FAILED);
        verify(podcastRepository, atLeast(2)).findById(eq(podcast.getId()));
        verify(itemRepository, atLeast(2)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
    }

    @Test
    public void should_handle_downloadunterruptedError() throws MalformedURLException {
        /* Given */
        httpDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List(item.getUrl())).build());

        DownloadInfo downloadInfo = mock(DownloadInfo.class);
        WGet wGet = mock(WGet.class);

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(urlService.getRealURL(anyString())).then(i -> i.getArguments()[0]);
        when(wGetFactory.newDownloadInfo(anyString())).thenReturn(downloadInfo);
        when(wGetFactory.newWGet(any(DownloadInfo.class), any(File.class))).thenReturn(wGet);
        doThrow(DownloadInterruptedError.class).when(wGet).download(any(AtomicBoolean.class), any(HTTPWatcher.class));

        /* When */
        httpDownloader.run();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STARTED);
        verify(podcastRepository, atLeast(1)).findById(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
    }

    @Test
    public void should_handle_IOException_during_download() throws MalformedURLException {
        /* Given */
        httpDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List(item.getUrl())).build());

        DownloadInfo downloadInfo = mock(DownloadInfo.class);
        WGet wGet = mock(WGet.class);

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(urlService.getRealURL(anyString())).then(i -> i.getArguments()[0]);
        when(wGetFactory.newDownloadInfo(anyString())).thenReturn(downloadInfo);
        when(wGetFactory.newWGet(any(DownloadInfo.class), any(File.class))).thenReturn(wGet);
        doThrow(UncheckedIOException.class).when(wGet).download(any(AtomicBoolean.class), any(HTTPWatcher.class));

        /* When */
        httpDownloader.run();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FAILED);
        verify(podcastRepository, atLeast(2)).findById(eq(podcast.getId()));
        verify(itemRepository, atLeast(2)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
    }

}
