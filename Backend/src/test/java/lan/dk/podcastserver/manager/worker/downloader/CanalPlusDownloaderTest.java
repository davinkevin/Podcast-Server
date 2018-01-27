package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.manager.worker.downloader.model.DownloadingItem;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.vavr.API.List;
import static io.vavr.API.Try;
import static lan.dk.utils.IOUtils.ROOT_TEST_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 16/08/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class CanalPlusDownloaderTest {

    private @Mock PodcastRepository podcastRepository;
    private @Mock ItemRepository itemRepository;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SimpMessagingTemplate template;
    private @Mock MimeTypeService mimeTypeService;
    private @Mock ItemDownloadManager itemDownloadManager;

    private @Mock UrlService urlService;
    private @Mock M3U8Service m3U8Service;
    private @Mock FfmpegService ffmpegService;
    private @Mock ProcessService processService;

    private @InjectMocks CanalPlusDownloader canalPlusDownloader;

    @Before
    public void beforeEach() {
        when(podcastServerParameters.getDownloadExtension()).thenReturn(".psdownload");
        canalPlusDownloader.postConstruct();

        FileSystemUtils.deleteRecursively(ROOT_TEST_PATH.resolve("Cplus Podcast").toFile());
        Try(() -> Files.createDirectories(ROOT_TEST_PATH));
    }

    @Test
    public void should_get_target_file_for_cplus() {
        /* Given */
        canalPlusDownloader.setDownloadingItem(DownloadingItem.builder()
                .item(Item.builder()
                        .url("http://us-cplus-aka.canal-plus.com/i/1401/NIP_1960_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8")
                        .podcast(Podcast.builder().title("Cplus Podcast").build())
                        .build()
                )
                .urls(List())
                .build()
        );
        when(podcastServerParameters.getRootfolder()).thenReturn(IOUtils.ROOT_TEST_PATH);

        /* When */
        Path targetFile = canalPlusDownloader.getTargetFile(canalPlusDownloader.getItem());

        /* Then */
        assertThat(targetFile).isEqualTo(IOUtils.ROOT_TEST_PATH.resolve("Cplus Podcast").resolve("NIP_1960_1500k.mp4.psdownload"));
    }
}
