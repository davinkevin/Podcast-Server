package lan.dk.podcastserver.manager.worker.dailymotion;

import com.github.davinkevin.podcastserver.service.M3U8Service;
import com.github.davinkevin.podcastserver.service.UrlService;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.service.JsonService;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.davinkevin.podcastserver.IOUtils.stringAsJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 24/12/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class DailymotionExtractorTest {

    private @Mock UrlService urlService;
    private @Mock JsonService jsonService;
    private @Mock M3U8Service m3U8Service;
    private @InjectMocks
    DailymotionExtractor extractor;

    private Item item;

    @Before
    public void beforeEach() {
        item = Item.builder()
                .title("Title")
                .url("http://a.fake.url/with/file.mp4?param=1")
                .status(Status.NOT_DOWNLOADED)
            .build();
    }

    @Test
    public void should_load_chromecast_stream() {
        /* Given */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequest(WithResponseFrom("karimdebbache.dailymotion.html")));
        when(jsonService.parse(anyString())).then(i -> stringAsJson(i.getArgument(0)));
        when(m3U8Service.getM3U8UrlFormMultiStreamFile(anyString())).then(i -> "https://proxy-005.dc3.dailymotion.com/sec(c261ed40cc95bcf93923ccd7f1c92a83)/video/574/494/277494475_mp4_h264_aac_fhd.m3u8#cell=core&comment=QOEABR17&hls_maxMaxBufferLength=105");

        /* When */
        DownloadingItem downloadingItem = extractor.extract(item);

        /* Then */
        assertThat(downloadingItem.getUrls()).containsOnly("https://proxy-005.dc3.dailymotion.com/sec(c261ed40cc95bcf93923ccd7f1c92a83)/video/574/494/277494475_mp4_h264_aac_fhd.m3u8");
        assertThat(downloadingItem.getItem()).isSameAs(item);
        assertThat(downloadingItem.getFilename()).isEqualTo("file.mp4");
        verify(jsonService, times(1)).parse(anyString());
        verify(m3U8Service, times(1)).getM3U8UrlFormMultiStreamFile(anyString());
    }

    @Test
    public void should_warn_when_structure_of_page_change_and_return_null() {
        /* GIVEN */
        when(urlService.get(eq(item.getUrl()))).then(i -> mockGetRequest(WithResponseFrom("incoherent.dailymotion.html")));

        /* When */
        assertThatThrownBy(() -> extractor.extract(item)).isInstanceOf(RuntimeException.class)
            .withFailMessage("Url not found for http://a.fake.url/with/file.mp4?param=1");

        /* Then */
        verify(jsonService, never()).parse(anyString());
        verify(m3U8Service, never()).getM3U8UrlFormMultiStreamFile(anyString());
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(extractor.compatibility("http://a.fake.url/with/file.mp4?param=1")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_be_compatible() {
        assertThat(extractor.compatibility("https://dailymotion.com/video/foo/bar")).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    private GetRequest mockGetRequest(String uri) throws UnirestException {
        GetRequest request = mock(GetRequest.class);
        HttpResponse response = (HttpResponse<String>) mock(HttpResponse.class);
        when(request.asString()).thenReturn(response);
        when(response.getBody()).then(i -> IOUtils.fileAsString(uri));
        return request;
    }

    private static String WithResponseFrom(String filename) {
        return "/remote/podcast/dailymotion/" + filename;
    }

}