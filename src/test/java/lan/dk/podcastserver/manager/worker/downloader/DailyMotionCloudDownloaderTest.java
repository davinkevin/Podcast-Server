package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.UrlService;
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
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 23/01/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DailyMotionCloudDownloaderTest {

    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock ItemDownloadManager itemDownloadManager;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    @Mock UrlService urlService;
    @InjectMocks DailyMotionCloudDownloader dailyMotionCloudDownloader;

    @Test
    public void should_get_real_url_for_an_item() throws IOException, URISyntaxException {
        /* Given */
        Item item = new Item().setUrl("http://www.dailymotion.com/id/of/a/video");
        String realUrl = "http://proxy-91.dailymotion.com/video/221/442/9dce76b19072beda39720aa04aa2e47a-video=1404000-audio_AACL_fra_70000_315=70000.m3u8";
        when(urlService.getRealURL(eq(item.getUrl()))).thenReturn(item.getUrl());
        when(urlService.urlAsReader(eq(item.getUrl()))).thenReturn(getAsReader("/remote/downloader/dailymotion/dailymotion.m3u8"));
        when(urlService.urlWithDomain(eq(item.getUrl()), eq(realUrl))).thenReturn(realUrl);
        dailyMotionCloudDownloader.item = item;

        /* When */
        String resolvedUrl = dailyMotionCloudDownloader.getItemUrl();

        /* Then */
        assertThat(resolvedUrl).isEqualTo(realUrl);
    }

    @Test
    public void should_return_empty_if_url_isnt_found() throws IOException, URISyntaxException {
        /* Given */
        Item item = new Item().setUrl("http://www.dailymotion.com/id/of/a/video");
        when(urlService.getRealURL(eq(item.getUrl()))).thenReturn(item.getUrl());
        doThrow(IOException.class).when(urlService).urlAsReader(eq(item.getUrl()));
        dailyMotionCloudDownloader.item = item;

        /* When */
        String resolvedUrl = dailyMotionCloudDownloader.getItemUrl();

        /* Then */
        assertThat(resolvedUrl).isEqualTo("");
    }

    @Test
    public void should_return_already_fetched_url() {
        /* Given */
        dailyMotionCloudDownloader.redirectionUrl = "alreadyExistingUrl";

        /* When */
        String itemUrl = dailyMotionCloudDownloader.getItemUrl();

        /* Then */
        assertThat(itemUrl).isEqualTo("alreadyExistingUrl");
    }


    private BufferedReader getAsReader(String url) throws URISyntaxException, IOException {
        Path path = Paths.get(DailyMotionCloudDownloaderTest.class.getResource(url).toURI());
        return Files.newBufferedReader(path);
    }

}