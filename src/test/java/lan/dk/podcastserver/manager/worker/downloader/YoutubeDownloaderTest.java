package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.factory.WGetFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static lan.dk.podcastserver.manager.worker.downloader.AbstractDownloader.WS_TOPIC_DOWNLOAD;
import static lan.dk.podcastserver.manager.worker.downloader.AbstractDownloader.WS_TOPIC_PODCAST;
import static lan.dk.podcastserver.manager.worker.downloader.DownloaderTest.ROOT_FOLDER;
import static lan.dk.podcastserver.manager.worker.downloader.DownloaderTest.TEMPORARY_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 13/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class YoutubeDownloaderTest {

    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock ItemDownloadManager itemDownloadManager;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    @Mock WGetFactory wGetFactory;
    @Mock UrlService urlService;
    @InjectMocks YoutubeDownloader youtubeDownloader;

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

        when(podcastServerParameters.getDownloadExtension()).thenReturn(TEMPORARY_EXTENSION);

        youtubeDownloader.postConstruct();
        youtubeDownloader.setItemDownloadManager(itemDownloadManager);

        FileSystemUtils.deleteRecursively(Paths.get(ROOT_FOLDER, podcast.getTitle()).toFile());
    }

    @Test
    public void should_run_download() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item);

        VGetParser vGetParser = mock(VGetParser.class);
        VideoInfo videoInfo = mock(VideoInfo.class);
        VGet vGet = mock(VGet.class, RETURNS_SMART_NULLS);
        DownloadInfo info = mock(DownloadInfo.class);

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        doAnswer(i -> {
            Files.createFile(Paths.get(ROOT_FOLDER, podcast.getTitle(), "A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION));
            item.setStatus(Status.FINISH);
            youtubeDownloader.downloadInfo = info;
            youtubeDownloader.finishDownload();
            return null;
        })
                .when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        when(vGet.getTarget()).then(i -> youtubeDownloader.target);
        when(info.getContentType()).thenReturn("video/mp4");

        /* When */
        youtubeDownloader.run();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FINISH);
        verify(podcastRepository, atLeast(1)).findOne(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        verify(template, atLeast(1)).convertAndSend(eq(String.format(WS_TOPIC_PODCAST, podcast.getId())), same(item));
        verify(vGet, times(1)).extract(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        verify(vGet, times(1)).setTarget(any(File.class));
        verify(vGet, times(1)).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        assertThat(youtubeDownloader.target.toString()).isEqualTo("/tmp/A Fake Podcast/A_super_Name_of_Youtube-Video.mp4");
        assertThat(Files.exists(youtubeDownloader.target.toPath())).isTrue();
        assertThat(Files.exists(youtubeDownloader.target.toPath().resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION))).isFalse();
    }

    @Test(expected = RuntimeException.class)
    public void should_stop_if_get_target_throw_exception() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item);

        VGetParser vGetParser = mock(VGetParser.class);
        VideoInfo videoInfo = mock(VideoInfo.class);
        VGet vGet = mock(VGet.class, RETURNS_SMART_NULLS);
        DownloadInfo info = mock(DownloadInfo.class);

        when(itemDownloadManager.getRootfolder()).thenReturn("/bin/foo/");
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        when(vGet.getTarget()).then(i -> youtubeDownloader.target);
        when(info.getContentType()).thenReturn("video/mp4");

        /* When */
        youtubeDownloader.download();
    }

    @Test(expected = RuntimeException.class)
    public void should_handle_exception_during_finish_download() throws MalformedURLException {
        /* Given */
        podcast.setTitle("bin");
        youtubeDownloader.setItem(item.setUrl("http://foo.bar.com/bash"));

        VGetParser vGetParser = mock(VGetParser.class);
        VideoInfo videoInfo = mock(VideoInfo.class);
        VGet vGet = mock(VGet.class, RETURNS_SMART_NULLS);
        DownloadInfo info = mock(DownloadInfo.class);

        when(itemDownloadManager.getRootfolder()).thenReturn("/");
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("bash");
        when(vGet.getTarget()).then(i -> youtubeDownloader.target);
        when(info.getContentType()).thenReturn("video/");
        youtubeDownloader.downloadInfo = info;

        /* When */
        youtubeDownloader.download();
        youtubeDownloader.finishDownload();
    }

    @Test
    public void should_handle_multipart_error() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item);

        VGetParser vGetParser = mock(VGetParser.class);
        VideoInfo videoInfo = mock(VideoInfo.class);
        VGet vGet = mock(VGet.class, RETURNS_SMART_NULLS);
        DownloadInfo info = mock(DownloadInfo.class);

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        doThrow(new DownloadMultipartError(info)).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
        verify(podcastRepository, atLeast(1)).findOne(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        verify(template, atLeast(1)).convertAndSend(eq(String.format(WS_TOPIC_PODCAST, podcast.getId())), same(item));
        verify(vGet, times(1)).extract(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        verify(vGet, times(1)).setTarget(any(File.class));
        verify(vGet, times(1)).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        assertThat(Files.exists(youtubeDownloader.target.toPath())).isFalse();
        assertThat(Files.exists(youtubeDownloader.target.toPath().resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION))).isFalse();
    }

    @Test
    public void should_handle_Interruption_error() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item.setStatus(Status.STARTED));

        VGetParser vGetParser = mock(VGetParser.class);
        VideoInfo videoInfo = mock(VideoInfo.class);
        VGet vGet = mock(VGet.class, RETURNS_SMART_NULLS);

        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        doThrow(DownloadInterruptedError.class).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STARTED);
        verify(podcastRepository, never()).findOne(eq(podcast.getId()));
        verify(itemRepository, never()).save(eq(item));
        verify(template, never()).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        verify(template, never()).convertAndSend(eq(String.format(WS_TOPIC_PODCAST, podcast.getId())), same(item));
        verify(vGet, times(1)).extract(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        verify(vGet, times(1)).setTarget(any(File.class));
        verify(vGet, times(1)).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        assertThat(Files.exists(youtubeDownloader.target.toPath())).isFalse();
        assertThat(Files.exists(youtubeDownloader.target.toPath().resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION))).isFalse();
    }

    @Test
    public void should_reset_download_if_exception_happen() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item.setStatus(Status.STARTED));

        VGetParser vGetParser = mock(VGetParser.class);
        VideoInfo videoInfo = mock(VideoInfo.class);
        VGet vGet = mock(VGet.class, RETURNS_SMART_NULLS);

        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);
        when(itemDownloadManager.canBeReseted(eq(item))).thenReturn(true);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        doThrow(StringIndexOutOfBoundsException.class).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STARTED);
        verify(podcastRepository, never()).findOne(eq(podcast.getId()));
        verify(itemRepository, never()).save(eq(item));
        verify(template, never()).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        verify(template, never()).convertAndSend(eq(String.format(WS_TOPIC_PODCAST, podcast.getId())), same(item));
        verify(itemDownloadManager, times(1)).resetDownload(eq(item));
        verify(vGet, times(1)).extract(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        verify(vGet, times(1)).setTarget(any(File.class));
        verify(vGet, times(1)).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        assertThat(Files.exists(youtubeDownloader.target.toPath())).isFalse();
        assertThat(Files.exists(youtubeDownloader.target.toPath().resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION))).isFalse();
    }

    @Test
    public void should_stop_download_if_not_resetable() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item.setStatus(Status.STARTED));

        VGetParser vGetParser = mock(VGetParser.class);
        VideoInfo videoInfo = mock(VideoInfo.class);
        VGet vGet = mock(VGet.class);

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);
        when(itemDownloadManager.canBeReseted(eq(item))).thenReturn(false);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        doThrow(StringIndexOutOfBoundsException.class).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
        verify(podcastRepository, atLeast(1)).findOne(eq(podcast.getId()));
        verify(itemRepository, atLeast(1)).save(eq(item));
        verify(template, atLeast(1)).convertAndSend(eq(WS_TOPIC_DOWNLOAD), same(item));
        verify(template, atLeast(1)).convertAndSend(eq(String.format(WS_TOPIC_PODCAST, podcast.getId())), same(item));
        verify(vGet, times(1)).extract(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        verify(vGet, times(1)).setTarget(any(File.class));
        verify(vGet, times(1)).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));
        assertThat(Files.exists(youtubeDownloader.target.toPath())).isFalse();
        assertThat(Files.exists(youtubeDownloader.target.toPath().resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION))).isFalse();
    }
}