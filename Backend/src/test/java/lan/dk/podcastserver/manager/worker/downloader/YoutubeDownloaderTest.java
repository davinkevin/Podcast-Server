package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import io.vavr.collection.HashSet;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.FfmpegService;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.factory.WGetFactory;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.FileSystemUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.axet.vget.info.VideoInfo.States.DONE;
import static com.github.axet.vget.info.VideoInfo.States.DOWNLOADING;
import static com.jayway.awaitility.Awaitility.await;
import static io.vavr.API.Try;
import static java.util.concurrent.CompletableFuture.runAsync;
import static lan.dk.podcastserver.manager.worker.downloader.DownloaderTest.ROOT_FOLDER;
import static lan.dk.podcastserver.manager.worker.downloader.DownloaderTest.TEMPORARY_EXTENSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 13/02/2016 for Podcast Server
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class YoutubeDownloaderTest {

    private @Mock FfmpegService ffmpegService;
    private @Mock PodcastRepository podcastRepository;
    private @Mock ItemRepository itemRepository;
    private @Mock ItemDownloadManager itemDownloadManager;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SimpMessagingTemplate template;
    // private @Mock MimeTypeService mimeTypeService;
    private @Mock WGetFactory wGetFactory;
    private @InjectMocks YoutubeDownloader youtubeDownloader;

    private @Mock VideoInfo videoInfo;
    private @Mock VGetParser vGetParser;

    VGet vGet;
    Podcast podcast;
    Item item;
    private Path path;

    @Before
    public void beforeEach() throws MalformedURLException {
        item = new Item()
                .setTitle("Title")
                .setUrl("http://a.fake.url/with/file.mp4?param=1")
                .setStatus(Status.NOT_DOWNLOADED);
        podcast = Podcast.builder()
                .id(UUID.randomUUID())
                .title("A Fake Podcast")
                .items(HashSet.<Item>empty().toJavaSet())
                .build()
                .add(item);

        when(podcastServerParameters.getDownloadExtension()).thenReturn(TEMPORARY_EXTENSION);
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get(ROOT_FOLDER));
        vGet = mock(VGet.class, RETURNS_SMART_NULLS);
        when(vGet.getVideo()).thenReturn(videoInfo);

        youtubeDownloader.postConstruct();
        youtubeDownloader.setItemDownloadManager(itemDownloadManager);

        path = Paths.get(ROOT_FOLDER, podcast.getTitle());
        FileSystemUtils.deleteRecursively(path.toFile());
    }

    @Test
    public void should_run_download_with_low_quality_video() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item);

        when(vGet.getContentExt(any())).thenCallRealMethod();
        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        when(videoInfo.getState()).thenReturn(DONE);
        List<VideoFileInfo> videoList = generate(1);
        when(videoInfo.getInfo()).thenReturn(videoList);
        doAnswer(i -> {
            videoList.forEach(f ->  Try(() -> Files.createFile(f.targetFile.toPath())));
            i.getArgumentAt(2, Runnable.class).run();
            return null;
        }).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.run();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FINISH);
        assertThat(youtubeDownloader.target.toString()).isEqualTo("/tmp/A Fake Podcast/A_super_Name_of_Youtube-Video.mp4");
        assertThat(Files.exists(youtubeDownloader.target)).isTrue();
        assertThat(Files.exists(youtubeDownloader.target.resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION))).isFalse();
    }

    @Test
    public void should_run_download_with_multiple_files_for_audio_and_video() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item.setStatus(Status.STARTED));

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        when(ffmpegService.mergeAudioAndVideo(any(), any(), any())).then(i -> {
            Try(() -> Files.createFile(i.getArgumentAt(2, Path.class)));
            return i.getArgumentAt(2, Path.class);
        });
        List<VideoFileInfo> videoList = generate(2);
        when(videoInfo.getInfo()).thenReturn(videoList);
        doAnswer(simulateDownload(videoInfo, videoList)).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FINISH);
        assertThat(youtubeDownloader.target.toString()).isEqualTo("/tmp/A Fake Podcast/A_super_Name_of_Youtube-Video.mp4");
        assertThat(Files.exists(youtubeDownloader.target)).isTrue();
        assertThat(Files.exists(youtubeDownloader.target.resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION))).isFalse();
    }

    @Test(expected = RuntimeException.class)
    public void should_stop_if_get_target_throw_exception() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item);

        DownloadInfo info = mock(DownloadInfo.class);

        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/bin/foo/"));
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        when(videoInfo.getInfo()).thenReturn(generate(3));
        when(info.getContentType()).thenReturn("video/mp4");

        /* When */
        youtubeDownloader.download();
    }

    @Test(expected = RuntimeException.class)
    public void should_handle_exception_during_finish_download() throws MalformedURLException {
        /* Given */
        podcast.setTitle("bin");
        youtubeDownloader.setItem(item.setUrl("http://foo.bar.com/bash"));
        youtubeDownloader.v = vGet;
        youtubeDownloader.target = Paths.get("/bin/bash");

        /* When */
        youtubeDownloader.finishDownload();
    }

    @Test
    public void should_handle_multipart_error() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item);

        DownloadInfo info = mock(DownloadInfo.class);

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        when(videoInfo.getInfo()).thenReturn(generate(1));
        doThrow(new DownloadMultipartError(info)).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
        assertThat(Files.exists(youtubeDownloader.target)).isFalse();
        assertThat(Files.exists(youtubeDownloader.target.resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION))).isFalse();
    }
    
    @Test(expected = RuntimeException.class)
    public void should_handle_error_during_merging_of_video_and_audio() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item.setStatus(Status.STARTED));

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast.setTitle("bin"));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/"));
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("bash");
        when(videoInfo.getInfo()).thenReturn(generate(2));
        doAnswer(simulateDownload(videoInfo, generate(2))).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        /* See exception */
    }


    @Test
    public void should_pause() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item.setStatus(Status.STARTED));

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        List<VideoFileInfo> videoList = generate(1);
        when(videoInfo.getInfo()).thenReturn(videoList);
        doAnswer(simulateDownload(videoInfo, videoList)).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        runAsync(() -> youtubeDownloader.download());
        youtubeDownloader.pauseDownload();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.PAUSED);
    }

    @Test
    public void should_handle_Interruption_error() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item.setStatus(Status.STARTED));

        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        doThrow(DownloadInterruptedError.class).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STARTED);
    }

    @Test
    public void should_reset_download_if_exception_happen() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item.setStatus(Status.STARTED));

        when(itemDownloadManager.canBeReset(eq(item))).thenReturn(true);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        doThrow(StringIndexOutOfBoundsException.class).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STARTED);
    }

    @Test
    public void should_stop_download_if_not_resetable() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setItem(item.setStatus(Status.STARTED));

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(itemDownloadManager.canBeReset(eq(item))).thenReturn(false);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video");
        when(videoInfo.getInfo()).thenReturn(generate(1));
        doThrow(StringIndexOutOfBoundsException.class).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.STOPPED);
        assertThat(youtubeDownloader.target).doesNotExist();
        assertThat(youtubeDownloader.target.resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION)).doesNotExist();
    }

    @Test
    public void should_delete_all_files_if_stopped_during_pause() {
        /* Given */
        when(videoInfo.getInfo()).thenReturn(generate(1));
        youtubeDownloader.item = item.setStatus(Status.PAUSED);

        /* When */
        runAsync(() -> {
            synchronized (youtubeDownloader.watcher){
                Try.run(youtubeDownloader::wait);
                item.setTitle("FooBar");
            }
        });
        youtubeDownloader.stopDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(item.getTitle()).isEqualTo("FooBar");
        });
    }

    @Test
    public void should_restart_download() {
        /* Given */
        when(videoInfo.getInfo()).thenReturn(generate(1));
        youtubeDownloader.item = item.setStatus(Status.PAUSED);

        /* When */
        runAsync(() -> {
            synchronized (youtubeDownloader.watcher){
                Try.run(youtubeDownloader::wait);
                item.setTitle("FooBar");
            }
        });
        youtubeDownloader.restartDownload();

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(item.getTitle()).isEqualTo("FooBar");
            assertThat(item.getStatus()).isEqualTo(Status.STARTED);
        });
    }

    private List<VideoFileInfo> generate(Integer number) {
        return IntStream
                .range(0, number)
                .mapToObj(i -> {
                    VideoFileInfo v = new VideoFileInfo(null);
                    Path file = path.resolve("tmp_" + i + ".tmp");
                    v.targetFile =  file.toFile();
                    v.setContentType( i == 0 ? "video/mp4" : "audio/webm");
                    v.setLength(i*1000L);
                    return v;
                }).collect(Collectors.toList());

    }

    private Answer simulateDownload(VideoInfo videoInfo, List<VideoFileInfo> videoList) {
        return i -> {
            videoList.forEach(f ->  Try(() -> Files.createFile(f.targetFile.toPath())));
            for (int cpt = 0; cpt <= 100; cpt++) {
                int finalCpt = cpt;
                when(videoInfo.getState()).thenReturn(finalCpt < 100 ? DOWNLOADING : DONE);
                videoList.stream().forEach(v -> v.setCount((v.getLength() / 100)*finalCpt));
                i.getArgumentAt(2, Runnable.class).run();
            }
            return null;
        };
    }
}
