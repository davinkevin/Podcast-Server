package lan.dk.podcastserver.manager.worker.downloader;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import io.vavr.collection.HashSet;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.M3U8Service;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 22/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class DailymotionDownloaderTest {

    // private @Mock PodcastRepository podcastRepository;
    // private @Mock ItemRepository itemRepository;
    // private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock ItemDownloadManager itemDownloadManager;
    // private @Mock SimpMessagingTemplate template;
    // private @Mock MimeTypeService mimeTypeService;
    private @Mock UrlService urlService;
    // private @Mock WGetFactory wGetFactory;
    private @Mock JsonService jsonService;
    private @Mock M3U8Service m3U8Service;
    private @InjectMocks DailymotionDownloader dailymotionDownloader;

    Item item;
    Podcast podcast;

    @Before
    public void beforeEach() {
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

        dailymotionDownloader.setItem(item);
        dailymotionDownloader.setItemDownloadManager(itemDownloadManager);
    }

    @Test
    public void should_load_chromecast_stream() throws URISyntaxException, IOException, UnirestException {
        /* Given */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequestWithStringResponse("/remote/downloader/dailymotion/karimdebbache.dailymotion.html"));
        when(jsonService.parse(anyString())).then(i -> JsonPath.using(Configuration.builder().mappingProvider(new JacksonMappingProvider()).build()).parse(i.getArgumentAt(0, String.class)));
        when(m3U8Service.getM3U8UrlFormMultiStreamFile(anyString())).then(i -> "https://proxy-005.dc3.dailymotion.com/sec(c261ed40cc95bcf93923ccd7f1c92a83)/video/574/494/277494475_mp4_h264_aac_fhd.m3u8#cell=core&comment=QOEABR17&hls_maxMaxBufferLength=105");

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("https://proxy-005.dc3.dailymotion.com/sec(c261ed40cc95bcf93923ccd7f1c92a83)/video/574/494/277494475_mp4_h264_aac_fhd.m3u8");
        verify(jsonService, times(1)).parse(anyString());
        verify(m3U8Service, times(1)).getM3U8UrlFormMultiStreamFile(anyString());
    }

    @Test
    public void should_warn_when_structure_of_page_change_and_return_null() {
        /* GIVEN */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequestWithStringResponse("/remote/downloader/dailymotion/incoherent.dailymotion.html"));

        /* When */
        assertThatThrownBy(() -> dailymotionDownloader.getItemUrl(item)).isInstanceOf(RuntimeException.class)
            .withFailMessage("Url not found for http://a.fake.url/with/file.mp4?param=1");

        /* Then */
        verify(jsonService, never()).parse(anyString());
        verify(m3U8Service, never()).getM3U8UrlFormMultiStreamFile(anyString());
    }

    @Test
    public void should_return_same_object_for_two_invocation() {
        /* Given */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequestWithStringResponse("/remote/downloader/dailymotion/karimdebbache.dailymotion.html"));
        when(jsonService.parse(anyString())).then(i -> JsonPath.using(Configuration.builder().mappingProvider(new JacksonMappingProvider()).build()).parse(i.getArgumentAt(0, String.class)));
        when(m3U8Service.getM3U8UrlFormMultiStreamFile(anyString())).then(i -> "https://proxy-005.dc3.dailymotion.com/sec(c261ed40cc95bcf93923ccd7f1c92a83)/video/574/494/277494475_mp4_h264_aac_fhd.m3u8#cell=core&comment=QOEABR17&hls_maxMaxBufferLength=105");

        /* When */
        String itemUrl = dailymotionDownloader.getItemUrl(item);
        String anotherItemUrl = dailymotionDownloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("https://proxy-005.dc3.dailymotion.com/sec(c261ed40cc95bcf93923ccd7f1c92a83)/video/574/494/277494475_mp4_h264_aac_fhd.m3u8");
        assertThat(itemUrl).isSameAs(anotherItemUrl);
        verify(jsonService, times(1)).parse(anyString());
        verify(m3U8Service, times(1)).getM3U8UrlFormMultiStreamFile(anyString());
    }

    @SuppressWarnings("unchecked")
    private GetRequest mockGetRequestWithStringResponse(String uri) throws UnirestException {
        GetRequest request = mock(GetRequest.class);
        HttpResponse response = (HttpResponse<String>) mock(HttpResponse.class);
        when(request.asString()).thenReturn(response);
        when(response.getBody()).then(i -> IOUtils.fileAsString(uri));
        return request;
    }
}
