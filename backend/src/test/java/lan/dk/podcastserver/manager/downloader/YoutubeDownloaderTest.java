package lan.dk.podcastserver.manager.downloader;

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
import com.github.davinkevin.podcastserver.service.FfmpegService;
import com.github.davinkevin.podcastserver.service.factory.WGetFactory;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import com.github.davinkevin.podcastserver.IOUtils;
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
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
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
import static lan.dk.podcastserver.manager.downloader.DownloaderTest.TEMPORARY_EXTENSION;
import static com.github.davinkevin.podcastserver.IOUtils.ROOT_TEST_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private VGet vGet;
    private Podcast podcast;
    private Item item;
    private Path path;

    @Before
    public void beforeEach() throws MalformedURLException {
        item = Item.builder()
                    .title("Title")
                    .url("http://a.fake.url/with/file.mp4?param=1")
                    .status(Status.NOT_DOWNLOADED)
                    .numberOfFail(0)
                    .progression(0)
                .build();
        podcast = Podcast.builder()
                    .id(UUID.randomUUID())
                    .title("A Fake Youtube Podcast")
                    .items(HashSet.<Item>empty().toJavaSet())
                .build()
                .add(item);

        when(podcastServerParameters.getDownloadExtension()).thenReturn(TEMPORARY_EXTENSION);
        when(podcastServerParameters.getRootfolder()).thenReturn(IOUtils.ROOT_TEST_PATH);
        vGet = mock(VGet.class, RETURNS_SMART_NULLS);
        when(vGet.getVideo()).thenReturn(videoInfo);

        youtubeDownloader.postConstruct();
        youtubeDownloader.setItemDownloadManager(itemDownloadManager);

        path = IOUtils.ROOT_TEST_PATH.resolve(podcast.getTitle());
        FileSystemUtils.deleteRecursively(path.toFile());
        Try(() -> Files.createDirectories(ROOT_TEST_PATH));
    }

    @Test
    public void should_run_download_with_low_quality_video() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setDownloadingItem(DownloadingItem.builder().item(item).build());

        when(vGet.getContentExt(any())).thenCallRealMethod();
        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video low");
        when(videoInfo.getState()).thenReturn(DONE);
        List<VideoFileInfo> videoList = generate(1);
        when(videoInfo.getInfo()).thenReturn(videoList);
        doAnswer(i -> {
            videoList.forEach(f ->  Try(() -> Files.createFile(f.targetFile.toPath())));
            ((Runnable) i.getArgument(2)).run();
            return null;
        }).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.run();

        /* Then */
        assertThat(item.getStatus()).isEqualTo(Status.FINISH);
        assertThat(youtubeDownloader.target).isEqualTo(IOUtils.ROOT_TEST_PATH.resolve("A Fake Youtube Podcast").resolve("A_super_Name_of_Youtube-Video_low.mp4"));
        assertThat(Files.exists(youtubeDownloader.target)).isTrue();
        assertThat(Files.exists(youtubeDownloader.target.resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION))).isFalse();
    }

    @Test
    public void should_run_download_with_multiple_files_for_audio_and_video() throws MalformedURLException {
        /* Given */
        Item item = this.item.setStatus(Status.STARTED);
        youtubeDownloader.setDownloadingItem(DownloadingItem.builder().item(item).build());

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(wGetFactory.parser(eq(this.item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(this.item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video multiple");
        when(ffmpegService.mergeAudioAndVideo(any(), any(), any())).then(i -> {
            Try(() -> Files.createFile(i.getArgument(2)));
            return i.getArgument(2);
        });
        List<VideoFileInfo> videoList = generate(2);
        when(videoInfo.getInfo()).thenReturn(videoList);
        doAnswer(simulateDownload(videoInfo, videoList)).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(this.item.getStatus()).isEqualTo(Status.FINISH);
        assertThat(youtubeDownloader.target).isEqualTo(IOUtils.ROOT_TEST_PATH.resolve("A Fake Youtube Podcast").resolve("A_super_Name_of_Youtube-Video_multiple.mp4"));
        assertThat(Files.exists(youtubeDownloader.target)).isTrue();
        assertThat(Files.exists(youtubeDownloader.target.resolveSibling("A_super_Name_of_Youtube-Video" + TEMPORARY_EXTENSION))).isFalse();
    }

    @Test(expected = RuntimeException.class)
    public void should_stop_if_get_target_throw_exception() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setDownloadingItem(DownloadingItem.builder().item(item).build());

        DownloadInfo info = mock(DownloadInfo.class);

        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/bin/foo/"));
        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video stop");
        when(videoInfo.getInfo()).thenReturn(generate(3));

        /* When */
        youtubeDownloader.download();
    }

    @Test(expected = RuntimeException.class)
    public void should_handle_exception_during_finish_download() throws MalformedURLException {
        /* Given */
        podcast.setTitle("bin");
        Item item = this.item.setUrl("http://foo.bar.com/bash");
        youtubeDownloader.setDownloadingItem(DownloadingItem.builder().item(item).build());
        youtubeDownloader.v = vGet;
        youtubeDownloader.target = Paths.get("/bin/bash");

        /* When */
        youtubeDownloader.finishDownload();
    }

    @Test
    public void should_handle_multipart_error() throws MalformedURLException {
        /* Given */
        youtubeDownloader.setDownloadingItem(DownloadingItem.builder().item(item).build());

        DownloadInfo info = mock(DownloadInfo.class);

        when(wGetFactory.parser(eq(item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video multipart error");
        when(videoInfo.getInfo()).thenReturn(generate(1));
        doThrow(new DownloadMultipartError(info)).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        assertThatThrownBy(() -> youtubeDownloader.download())
        /* Then */
                .isInstanceOf(RuntimeException.class);
    }
    
    @Test
    public void should_handle_error_during_merging_of_video_and_audio() throws MalformedURLException {
        /* Given */
        Item item = this.item.setStatus(Status.STARTED);
        youtubeDownloader.setDownloadingItem(DownloadingItem.builder().item(item).build());

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast.setTitle("bin")));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/"));
        when(wGetFactory.parser(eq(this.item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(this.item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("bash");
        when(videoInfo.getInfo()).thenReturn(generate(2));

        /* When */
        assertThatThrownBy(() -> youtubeDownloader.download()).isInstanceOfAny(FileSystemException.class, Exception.class);
    }


    @Test
    public void should_pause() throws MalformedURLException {
        /* Given */
        Item item = this.item.setStatus(Status.STARTED);
        youtubeDownloader.setDownloadingItem(DownloadingItem.builder().item(item).build());

        when(podcastRepository.findById(eq(podcast.getId()))).thenReturn(Optional.of(podcast));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(wGetFactory.parser(eq(this.item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(this.item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video pause");
        List<VideoFileInfo> videoList = generate(1);
        when(videoInfo.getInfo()).thenReturn(videoList);
        doAnswer(simulateDownload(videoInfo, videoList)).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        runAsync(() -> youtubeDownloader.download());
        youtubeDownloader.pauseDownload();

        /* Then */
        assertThat(this.item.getStatus()).isEqualTo(Status.PAUSED);
    }

    @Test
    public void should_handle_Interruption_error() throws MalformedURLException {
        /* Given */
        Item item = this.item.setStatus(Status.STARTED);
        youtubeDownloader.setDownloadingItem(DownloadingItem.builder().item(item).build());

        when(wGetFactory.parser(eq(this.item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(this.item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video interruption");
        doThrow(DownloadInterruptedError.class).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        youtubeDownloader.download();

        /* Then */
        assertThat(this.item.getStatus()).isEqualTo(Status.STARTED);
    }

    @Test
    public void should_reset_download_if_exception_happen() throws MalformedURLException {
        /* Given */
        Item item = this.item.setStatus(Status.STARTED);
        youtubeDownloader.setDownloadingItem(DownloadingItem.builder().item(item).build());

        when(wGetFactory.parser(eq(this.item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(this.item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video reset");
        doThrow(StringIndexOutOfBoundsException.class).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        assertThatThrownBy(() -> youtubeDownloader.download())
        /* Then */
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void should_stop_download_if_not_resetable() throws MalformedURLException {
        /* Given */
        Item item = this.item.setStatus(Status.STARTED);
        youtubeDownloader.setDownloadingItem(DownloadingItem.builder().item(item).build());

        when(wGetFactory.parser(eq(this.item.getUrl()))).thenReturn(vGetParser);
        when(vGetParser.info(eq(new URL(this.item.getUrl())))).thenReturn(videoInfo);
        when(wGetFactory.newVGet(eq(videoInfo))).thenReturn(vGet);
        when(videoInfo.getTitle()).thenReturn("A super Name of Youtube-Video stop not resetable");
        when(videoInfo.getInfo()).thenReturn(generate(1));
        doThrow(StringIndexOutOfBoundsException.class).when(vGet).download(eq(vGetParser), any(AtomicBoolean.class), any(Runnable.class));

        /* When */
        assertThatThrownBy(() -> youtubeDownloader.download())
        /* Then */
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void should_delete_all_files_if_stopped_during_pause() {
        /* Given */
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
                ((Runnable) i.getArgument(2)).run();
            }
            return null;
        };
    }
}
