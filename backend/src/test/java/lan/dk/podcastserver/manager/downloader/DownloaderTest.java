package lan.dk.podcastserver.manager.downloader;

import com.github.davinkevin.podcastserver.service.MimeTypeService;
import io.vavr.collection.HashSet;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import static io.vavr.API.List;
import static io.vavr.API.Try;
import static lan.dk.podcastserver.manager.downloader.AbstractDownloader.WS_TOPIC_DOWNLOAD;
import static com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 09/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DownloaderTest {

    static final String TEMPORARY_EXTENSION = ".psdownload";

    private @Mock PodcastRepository podcastRepository;
    private @Mock ItemRepository itemRepository;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SimpMessagingTemplate template;
    // private @Mock MimeTypeService mimeTypeService;
    private @Mock ItemDownloadManager itemDownloadManager;
    private @InjectMocks SimpleDownloader simpleDownloader;


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
                    .title("A Fake typeless Podcast")
                    .items(HashSet.<Item>empty().toJavaSet())
                .build()
                .add(item);

        simpleDownloader.setItemDownloadManager(itemDownloadManager);
        when(podcastServerParameters.getDownloadExtension()).thenReturn(TEMPORARY_EXTENSION);
        when(podcastServerParameters.getRootfolder()).thenReturn(ROOT_TEST_PATH);
        simpleDownloader.postConstruct();

        Try(() -> Files.createDirectories(ROOT_TEST_PATH));
        FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve(podcast.getTitle()).toFile());
    }

    @Test
    public void should_stop_download() throws MalformedURLException {
        /* Given */
        simpleDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List()).build());

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        /* When */
        simpleDownloader.run();
        simpleDownloader.stopDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
        verify(podcastRepository, atLeast(1)).findById(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        assertThat(simpleDownloader.target).isEqualTo(IOUtils.ROOT_TEST_PATH.resolve("A Fake typeless Podcast").resolve("file.mp4" + TEMPORARY_EXTENSION));
    }

    @Test
    public void should_handle_finish_download_without_target() {
        /* Given */
        simpleDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List()).build());

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        /* When */
        simpleDownloader.finishDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FAILED);
        verify(podcastRepository, atLeast(1)).findById(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        assertThat(simpleDownloader.target).isNull();
    }

    @Test
    public void should_pause_a_download() {
        /* Given */
        simpleDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List()).build());

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        /* When */
        simpleDownloader.pauseDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.PAUSED);
        verify(podcastRepository, atLeast(1)).findById(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
    }

    @Test
    public void should_save_with_same_file_already_existing() throws MalformedURLException {
        /* Given */
        simpleDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List()).build());

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        /* When */
        simpleDownloader.run();
        simpleDownloader.finishDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FINISH);
        assertThat(item.getFileName()).isEqualTo("file.mp4");
        assertThat(simpleDownloader.target).isEqualTo(IOUtils.ROOT_TEST_PATH.resolve("A Fake typeless Podcast").resolve("file.mp4"));
    }

    @Test
    public void should_handle_exception_during_move() throws MalformedURLException {
        /* Given */
        simpleDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List()).build());

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        /* When */
        simpleDownloader.run();
        simpleDownloader.target = Paths.get("/tmp", podcast.getTitle(), "fake_file" + TEMPORARY_EXTENSION);
        simpleDownloader.finishDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FINISH);
    }

    @Test
    public void should_get_the_same_target_file_each_call() throws MalformedURLException {
        /* Given */
        simpleDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List()).build());

        /* When */
        simpleDownloader.target = simpleDownloader.getTargetFile(item);
        Path target2 = simpleDownloader.getTargetFile(item);

        /* Then */
        assertThat(simpleDownloader.target).isSameAs(target2);
    }

    @Test
    public void should_handle_duplicate_on_file_name() throws IOException {
        /* Given */
        simpleDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List()).build());
        Files.createDirectory(ROOT_TEST_PATH.resolve(podcast.getTitle()));
        Files.createFile(ROOT_TEST_PATH.resolve(podcast.getTitle()).resolve( "file.mp4" + TEMPORARY_EXTENSION));

        /* When */
        Path targetFile = simpleDownloader.getTargetFile(item);

        /* Then */
        assertThat(targetFile).isNotEqualTo(ROOT_TEST_PATH.resolve(podcast.getTitle()).resolve( "file.mp4" + TEMPORARY_EXTENSION));
    }

    @Test
    //@Ignore
    public void should_handle_error_during_creation_of_temp_file() {
        /* Given */
        podcast.setTitle("bin");
        simpleDownloader.setDownloadingItem(
                DownloadingItem.builder().item(item.setUrl("http://foo.bar.com/bash")).urls(List()).build());

        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/"));
        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        /* When */
        simpleDownloader.getTargetFile(item);

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
    }

    @Test
    public void should_save_sync_with_podcast() {
        /* Given */
        simpleDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List()).build());

        doThrow(RuntimeException.class).when(podcastRepository).findById(any(UUID.class));

        /* When */
        simpleDownloader.saveSyncWithPodcast();

        /* Then */
        assertThat(simpleDownloader.getItem()).isSameAs(item);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    public void should_get_item_standard() {
        /* Given */
        simpleDownloader.setDownloadingItem(DownloadingItem.builder().item(item).urls(List()).build());

        /* When */
        Item itemOfDownloader = simpleDownloader.getItem();

        /* Then */
        assertThat(item).isSameAs(itemOfDownloader);
    }

    static class SimpleDownloader extends AbstractDownloader {

        SimpleDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService) {
            super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService);
        }

        @Override
        public Item download() {
            try {
                target = getTargetFile(item);
                item.setStatus(Status.FINISH);
                Files.createFile(ROOT_TEST_PATH.resolve(item.getPodcast().getTitle()).resolve( "file.mp4" + TEMPORARY_EXTENSION));
                Files.createFile(ROOT_TEST_PATH.resolve(item.getPodcast().getTitle()).resolve( "file.mp4"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public Integer compatibility(DownloadingItem url) {
            return null;
        }
    }
}
