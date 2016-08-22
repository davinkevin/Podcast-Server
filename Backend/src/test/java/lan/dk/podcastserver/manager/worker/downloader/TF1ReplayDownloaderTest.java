package lan.dk.podcastserver.manager.worker.downloader;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import javaslang.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.service.*;
import lan.dk.utils.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Consumer;

import static lan.dk.podcastserver.manager.worker.downloader.TF1ReplayDownloader.TF1ReplayVideoUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 21/07/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class TF1ReplayDownloaderTest {

    @Mock M3U8Service m3U8Service;
    @Mock HtmlService htmlService;
    @Mock JsonService jsonService;
    @Mock SignatureService signatureService;
    @Mock UrlService urlService;
    @InjectMocks TF1ReplayDownloader downloader;

    @Test
    public void should_be_instance_of_type_m3u8() {
        /* Given */
        /* When */
        /* Then */
        assertThat(downloader).isInstanceOf(M3U8Downloader.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_get_url() throws IOException, URISyntaxException, UnirestException {
        /* Given */

        Item item = Item.builder().url("http://www.tf1.fr/tf1/19h-live/videos/19h-live-20-juillet-2016.html").build();
        when(htmlService.get(eq(item.getUrl()))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/tf1replay/19h-live.item.html"));

        HttpRequestWithBody apiRequest = mock(HttpRequestWithBody.class, RETURNS_DEEP_STUBS);
        HttpResponse apiResponse = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        when(urlService.post(anyString())).thenReturn(apiRequest);
        when(apiRequest.header(any(), any()).asString()).thenReturn(apiResponse);
        when(apiResponse.getBody()).thenReturn("{ \"code\": 200, \"message\" : \"http:\\/\\/www.wat.tv\\/get\\/iphone\\/13075615.m3u8?token=b35fc8b36d16b0110fd6220bd9df7164%2F576eb335&bwmin=400000&bwmax=1500000\"}");

        GetRequest getRequest = mock(GetRequest.class, RETURNS_DEEP_STUBS);
        HttpResponse serverTimeResponse = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        when(urlService.get("http://www.wat.tv/servertime")).thenReturn(getRequest);
        when(getRequest.asString()).thenReturn(serverTimeResponse);
        when(serverTimeResponse.getBody()).thenReturn("1469136109|d51b902b86678eb59dd7d05895a67ea1|7200");

        GetRequest m3u8request = mock(GetRequest.class, RETURNS_DEEP_STUBS);
        HttpResponse m3u8Response = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        when(urlService.get(anyString())).thenReturn(m3u8request);
        when(m3u8request.asString()).thenReturn(m3u8Response);
        when(m3u8Response.getRawBody()).thenReturn(new NullInputStream(1L));

        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        when(urlService.getRealURL(anyString(), any(Consumer.class))).thenReturn("http://ios.tf1.fr/");
        when(m3U8Service.findBestQuality(any())).thenReturn(Option.of("foo/bar/video.mp4"));
        when(urlService.addDomainIfRelative(anyString(), anyString())).thenCallRealMethod();

        /* When */
        String itemUrl = downloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://ios.tf1.fr/foo/bar/video.mp4");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_get_url_with_short_id() throws IOException, URISyntaxException, UnirestException {
        /* Given */

        Item item = Item.builder().url("http://www.tf1.fr/tf1/19h-live/videos/19h-live-20-juillet-2016.html").build();
        when(htmlService.get(eq(item.getUrl()))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/tf1replay/19h-live.short-id.item.html"));

        HttpRequestWithBody apiRequest = mock(HttpRequestWithBody.class, RETURNS_DEEP_STUBS);
        HttpResponse apiResponse = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        when(urlService.post(anyString())).thenReturn(apiRequest);
        when(apiRequest.asString()).thenReturn(apiResponse);
        when(apiResponse.getBody()).thenReturn("{ \"code\": 200, \"message\" : \"http:\\/\\/www.wat.tv\\/get\\/iphone\\/13184238.m3u8?token=b35fc8b36d16b0110fd6220bd9df7164%2F576eb335&bwmin=400000&bwmax=1500000\"");

        GetRequest getRequest = mock(GetRequest.class, RETURNS_DEEP_STUBS);
        HttpResponse serverTimeResponse = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        when(urlService.get("http://www.wat.tv/servertime")).thenReturn(getRequest);
        when(getRequest.asString()).thenReturn(serverTimeResponse);
        when(serverTimeResponse.getBody()).thenReturn("1469136109|d51b902b86678eb59dd7d05895a67ea1|7200");

        GetRequest m3u8request = mock(GetRequest.class, RETURNS_DEEP_STUBS);
        HttpResponse m3u8Response = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        when(urlService.get(anyString())).thenReturn(m3u8request);
        when(m3u8request.asString()).thenReturn(m3u8Response);
        when(m3u8Response.getBody()).thenReturn("#EXT-INF\n foo/bar/vid.mp4");

        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        when(urlService.getRealURL(anyString(), any(Consumer.class))).thenReturn("http://ios.tf1.fr/");
        when(m3U8Service.findBestQuality(any())).thenReturn(Option.of("/foo/bar/video.mp4"));
        when(urlService.addDomainIfRelative(anyString(), anyString())).thenReturn("http://ios.tf1.fr/foo/bar/video.mp4");

        /* When */
        String itemUrl = downloader.getItemUrl(item);

        /* Then */
        assertThat(itemUrl).isEqualTo("http://ios.tf1.fr/foo/bar/video.mp4");
    }

    @Test
    public void should_return_given_item_if_not_the_same() {
        /* Given */
        downloader.item = Item.builder().url("http://foo.bar.com").build();
        Item item = Item.builder().url("http://foo.bar.com/other").build();

        /* When */
        String url = downloader.getItemUrl(item);

        /* Then */
        assertThat(url).isEqualTo(item.getUrl());
    }

    @Test
    public void should_be_compatible() {
        /* Given */
        String url = "www.tf1.fr/tf1/19h-live/videos";
        /* When */
        Integer compatibility = downloader.compatibility(url);
        /* Then */
        assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        /* Given */
        String url = "www.tf1.com/foo/bar/videos";
        /* When */
        Integer compatibility = downloader.compatibility(url);
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_have_pojo_for_response() throws IOException, URISyntaxException {
        /* Given */
        /*
            Due to limitation in fluent API / Mocking, I can't reach the serialization of data from text to POJO, so
            I have to write test to check it independently
         */
        String json = "{ \"code\": 200, \"message\" : \"http:\\/\\/www.wat.tv\\/get\\/iphone\\/13075615.m3u8?token=b35fc8b36d16b0110fd6220bd9df7164%2F576eb335&bwmin=400000&bwmax=1500000\"}";

        /* When */
        TF1ReplayVideoUrl videoUrl = IOUtils.stringAsJson(json).read("$", TF1ReplayVideoUrl.class);

        /* Then */
        assertThat(videoUrl).isNotNull();
        assertThat(videoUrl.getMessage()).isEqualTo("http://www.wat.tv/get/iphone/13075615.m3u8?token=b35fc8b36d16b0110fd6220bd9df7164%2F576eb335&bwmin=400000&bwmax=1500000");
    }

    @Test
    public void should_use_a_user_agent_mobile() {
        /* Given */

        /* When */
        String ua = downloader.withUserAgent();

        /* Then */
        assertThat(ua).isEqualTo("AppleCoreMedia/1.0.0.10B400 (iPod; U; CPU OS 6_1_5 like Mac OS X; fr_fr)");
    }

}