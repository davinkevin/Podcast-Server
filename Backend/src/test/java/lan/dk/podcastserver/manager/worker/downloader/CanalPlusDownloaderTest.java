package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 16/08/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class CanalPlusDownloaderTest {

    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    @Mock ItemDownloadManager itemDownloadManager;

    @Mock UrlService urlService;
    @Mock M3U8Service m3U8Service;
    @Mock FfmpegService ffmpegService;
    @Mock ProcessService processService;

    @InjectMocks CanalPlusDownloader canalPlusDownloader;

    @Before
    public void beforeEach() {
        when(podcastServerParameters.getDownloadExtension()).thenReturn(".psdownload");
        canalPlusDownloader.postConstruct();
    }

    @Test
    public void should_get_target_file_for_cplus() {
        /* Given */
        canalPlusDownloader.item = Item.builder()
                .url("http://us-cplus-aka.canal-plus.com/i/1401/NIP_1960_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8")
                .podcast(Podcast.builder().title("Cplus Podcast").build())
            .build();
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get("/tmp"));

        /* When */
        Path targetFile = canalPlusDownloader.getTargetFile(canalPlusDownloader.item);

        /* Then */
        assertThat(targetFile).isEqualTo(Paths.get("/tmp", "Cplus Podcast", "NIP_1960_1500k.mp4.psdownload"));
    }
}