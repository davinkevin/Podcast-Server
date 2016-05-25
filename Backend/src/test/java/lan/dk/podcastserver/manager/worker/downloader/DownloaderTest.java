package lan.dk.podcastserver.manager.worker.downloader;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
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
import java.util.UUID;

import static lan.dk.podcastserver.manager.worker.downloader.AbstractDownloader.WS_TOPIC_DOWNLOAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 09/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DownloaderTest {

    public static final String ROOT_FOLDER = "/tmp/";
    public static final String TEMPORARY_EXTENSION = ".psdownload";

    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    @Mock ItemDownloadManager itemDownloadManager;
    @InjectMocks SimpleDownloader simpleDownloader;


    Podcast podcast;
    Item item;

    @Before
    public void beforeEach() {
        item = new Item()
                .setTitle("Title")
                .setUrl("http://a.fake.url/with/file.mp4?param=1")
                .setStatus(Status.NOT_DOWNLOADED);
        podcast = Podcast.builder()
                .id(UUID.randomUUID())
                .title("A Fake Podcast")
                .items(Sets.newHashSet())
                .build()
                .add(item);

        simpleDownloader.setItemDownloadManager(itemDownloadManager);
        when(podcastServerParameters.getDownloadExtension()).thenReturn(TEMPORARY_EXTENSION);
        simpleDownloader.postConstruct();

        FileSystemUtils.deleteRecursively(Paths.get(ROOT_FOLDER, podcast.getTitle()).toFile());
    }

    @Test
    public void should_stop_download() throws MalformedURLException {
        /* Given */
        simpleDownloader.setItem(item);

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);

        /* When */
        simpleDownloader.run();
        simpleDownloader.stopDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
        verify(podcastRepository, atLeast(1)).findOne(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        assertThat(simpleDownloader.target.toString()).isEqualTo(String.format("/tmp/A Fake Podcast/file.mp4%s", TEMPORARY_EXTENSION));
    }

    @Test
    public void should_handle_finish_download_without_target() {
        /* Given */
        simpleDownloader.setItem(item);

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        /* When */
        simpleDownloader.finishDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
        verify(podcastRepository, atLeast(1)).findOne(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        assertThat(simpleDownloader.target).isNull();
    }

    @Test
    public void should_pause_a_download() {
        /* Given */
        simpleDownloader.setItem(item);

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        /* When */
        simpleDownloader.pauseDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.PAUSED);
        verify(podcastRepository, atLeast(1)).findOne(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
    }

    @Test
    public void should_save_with_same_file_already_existing() throws MalformedURLException {
        /* Given */
        simpleDownloader.setItem(item);

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);

        /* When */
        simpleDownloader.run();
        simpleDownloader.finishDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FINISH);
        assertThat(item.getFileName()).isEqualTo("file.mp4");
        assertThat(simpleDownloader.target.toString()).isEqualTo("/tmp/A Fake Podcast/file.mp4");
    }

    @Test
    public void should_handle_exception_during_move() throws MalformedURLException {
        /* Given */
        simpleDownloader.setItem(item);

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);

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
        simpleDownloader.setItem(item);
        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);

        /* When */
        simpleDownloader.target = simpleDownloader.getTargetFile(item);
        Path target2 = simpleDownloader.getTargetFile(item);

        /* Then */
        assertThat(simpleDownloader.target).isSameAs(target2);
    }

    @Test
    public void should_handle_duplicate_on_file_name() throws IOException {
        /* Given */
        simpleDownloader.setItem(item);
        Files.createDirectory(Paths.get(ROOT_FOLDER, podcast.getTitle()));
        Files.createFile(Paths.get(ROOT_FOLDER, podcast.getTitle(), "file.mp4" + TEMPORARY_EXTENSION));

        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);

        /* When */
        Path targetFile = simpleDownloader.getTargetFile(item);

        /* Then */
        assertThat(targetFile).isNotEqualTo(Paths.get(ROOT_FOLDER, podcast.getTitle(), "file.mp4" + TEMPORARY_EXTENSION).toFile());
    }

    @Test
    //@Ignore
    public void should_handle_error_during_creation_of_temp_file() throws IOException {
        /* Given */
        podcast.setTitle("bin");
        simpleDownloader.setItem(item.setUrl("http://foo.bar.com/bash"));

        when(itemDownloadManager.getRootfolder()).thenReturn("/");
        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);

        /* When */
        simpleDownloader.getTargetFile(item);

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
    }

    @Test
    public void should_save_sync_with_podcast() {
        /* Given */
        simpleDownloader.setItem(item);

        doThrow(RuntimeException.class).when(podcastRepository).findOne(any(UUID.class));

        /* When */
        simpleDownloader.saveSyncWithPodcast();

        /* Then */
        assertThat(simpleDownloader.getItem()).isSameAs(item);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    public void should_get_item_standard() {
        /* Given */
        simpleDownloader.setItem(item);

        /* When */
        Item itemOfDownloader = simpleDownloader.getItem();

        /* Then */
        assertThat(item).isSameAs(itemOfDownloader);
    }

    static class SimpleDownloader extends AbstractDownloader {

        @Override
        public Item download() {
            try {
                target = getTargetFile(item);
                item.setStatus(Status.FINISH);
                Files.createFile(Paths.get(ROOT_FOLDER, item.getPodcast().getTitle(), "file.mp4" + TEMPORARY_EXTENSION));
                Files.createFile(Paths.get(ROOT_FOLDER, item.getPodcast().getTitle(), "file.mp4"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public Integer compatibility(String url) {
            return null;
        }
    }
}