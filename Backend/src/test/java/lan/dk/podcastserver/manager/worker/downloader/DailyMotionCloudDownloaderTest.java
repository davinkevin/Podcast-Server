package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.M3U8Service;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import static io.vavr.API.Option;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 23/01/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DailyMotionCloudDownloaderTest {

    /*
    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    */
    private @Mock UrlService urlService;
    private @Mock M3U8Service m3U8Service;
    private @Mock ItemDownloadManager itemDownloadManager;
    private @InjectMocks DailyMotionCloudDownloader dailyMotionCloudDownloader;

    @Before
    public void beforeEach() {
        dailyMotionCloudDownloader.setItemDownloadManager(itemDownloadManager);
    }

    @Test
    public void should_get_real_url_for_an_item() throws IOException, URISyntaxException {
        /* Given */
        Item item = new Item().setUrl("http://www.dailymotion.com/id/of/a/video");
        String realUrl = "http://proxy-91.dailymotion.com/video/221/442/9dce76b19072beda39720aa04aa2e47a-video=1404000-audio_AACL_fra_70000_315=70000.m3u8";
        when(urlService.getRealURL(eq(item.getUrl()))).thenReturn(item.getUrl());
        when(urlService.asStream(eq(item.getUrl()))).then(i -> IOUtils.fileAsStream("/remote/downloader/dailymotion/dailymotion.m3u8"));
        when(urlService.addDomainIfRelative(eq(item.getUrl()), eq(realUrl))).thenCallRealMethod();
        when(m3U8Service.findBestQuality(any())).thenReturn(Option("http://proxy-91.dailymotion.com/video/221/442/9dce76b19072beda39720aa04aa2e47a-video=1404000-audio_AACL_fra_70000_315=70000.m3u8"));
        dailyMotionCloudDownloader.item = item;

        /* When */
        String resolvedUrl = dailyMotionCloudDownloader.getItemUrl(item);

        /* Then */
        assertThat(resolvedUrl).isEqualTo(realUrl);
    }

    @Test
    public void should_throw_exception_if_not_found() throws IOException, URISyntaxException {
        /* Given */
        Item item = new Item().setUrl("http://www.dailymotion.com/id/of/a/video");
        when(urlService.getRealURL(eq(item.getUrl()))).thenReturn(item.getUrl());
        doThrow(IOException.class).when(urlService).asStream(eq(item.getUrl()));
        dailyMotionCloudDownloader.item = item;

        /* When */
        assertThatThrownBy(() -> dailyMotionCloudDownloader.getItemUrl(item)).isInstanceOf(RuntimeException.class)
            .withFailMessage("Url not found for " + item.getUrl());
    }

    @Test
    public void should_return_already_fetched_url() throws IOException {
        /* Given */
        Item item = new Item().setUrl("http://www.dailymotion.com/id/of/a/video");
        String realUrl = "http://proxy-91.dailymotion.com/video/221/442/9dce76b19072beda39720aa04aa2e47a-video=1404000-audio_AACL_fra_70000_315=70000.m3u8";
        when(urlService.getRealURL(eq(item.getUrl()))).thenReturn(item.getUrl());
        when(urlService.asStream(eq(item.getUrl()))).then(i -> IOUtils.fileAsStream("/remote/downloader/dailymotion/dailymotion.m3u8"));
        when(urlService.addDomainIfRelative(eq(item.getUrl()), eq(realUrl))).thenCallRealMethod();
        when(m3U8Service.findBestQuality(any())).thenReturn(Option("http://proxy-91.dailymotion.com/video/221/442/9dce76b19072beda39720aa04aa2e47a-video=1404000-audio_AACL_fra_70000_315=70000.m3u8"));
        dailyMotionCloudDownloader.item = item;

        /* When */
        String resolvedUrl = dailyMotionCloudDownloader.getItemUrl(item);
        String secondlyResolvedUrl = dailyMotionCloudDownloader.getItemUrl(item);

        /* Then */
        assertThat(resolvedUrl).isSameAs(secondlyResolvedUrl);

    }
}
