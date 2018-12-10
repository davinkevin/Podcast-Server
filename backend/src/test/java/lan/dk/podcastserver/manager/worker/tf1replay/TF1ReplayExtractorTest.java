package lan.dk.podcastserver.manager.worker.tf1replay;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.M3U8Service;
import com.github.davinkevin.podcastserver.service.UrlService;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.downloader.DownloadingItem;
import lan.dk.podcastserver.service.JsonService;
import com.github.davinkevin.podcastserver.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.function.Consumer;

import static io.vavr.API.Option;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 12/12/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class TF1ReplayExtractorTest {
    private @Mock M3U8Service m3U8Service;
    private @Mock HtmlService htmlService;
    private @Mock JsonService jsonService;
    private @Mock UrlService urlService;
    private @InjectMocks
    TF1ReplayExtractor extractor;

    private Item item = Item.builder().url("http://www.tf1.fr/tf1/19h-live/videos/19h-live-20-juillet-2016.html").build();

    @Test
    @SuppressWarnings("unchecked")
    public void should_get_real_url() throws IOException, URISyntaxException, UnirestException {
        /* Given */
        String url = "http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.m3u8?vk=MTMzMTU2NDUubTN1OA==&st=UycCudlvBB6aTcCG37_Ulw&e=1492276114&t=1492265314";

        // Fetch HTML
        when(htmlService.get(eq(item.getUrl()))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/tf1replay/19h-live.item.html"));

        // Fetch WAT WEB_HTML
        GetRequest apiRequest = mock(GetRequest.class);
        HttpResponse apiResponse = mock(HttpResponse.class);
        when(urlService.get("http://www.wat.tv/get/webhtml/13184238")).thenReturn(apiRequest);
        when(apiRequest.header(any(), any())).thenReturn(apiRequest);
        when(apiRequest.asString()).thenReturn(apiResponse);
        when(apiResponse.getBody()).thenReturn("{\n" +
                "  \"hls\": \"http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.m3u8?vk=MTMzMTU2NDUubTN1OA==&st=UycCudlvBB6aTcCG37_Ulw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001\",\n" +
                "  \"mpd\": \"http://das-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.mpd?vk=MTMzMTU2NDUubXBk&st=SFYRoQwZsQ-84qF0vnX6Sw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001\"\n" +
                "}");

        GetRequest m3u8request = mock(GetRequest.class, RETURNS_DEEP_STUBS);
        HttpResponse m3u8Response = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        when(urlService.get(url)).thenReturn(m3u8request);
        when(m3u8request.header(anyString(), anyString())).thenReturn(m3u8request);
        when(m3u8request.asString()).thenReturn(m3u8Response);
        when(m3u8Response.getRawBody()).thenReturn(new NullInputStream(1L));

        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgument(0)));
        when(urlService.getRealURL(anyString(), any(Consumer.class))).thenReturn(url);
        when(m3U8Service.findBestQuality(any(InputStream.class))).thenReturn(Option("foo/bar/video.mp4"));
        when(urlService.addDomainIfRelative(anyString(), anyString())).thenCallRealMethod();

        /* When */
        DownloadingItem downloadingItem = extractor.extract(item);

        /* Then */
        assertThat(downloadingItem.url()).contains("http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/foo/bar/video.mp4");
        assertThat(downloadingItem.getFilename()).isEqualToIgnoringCase("19h-live-20-juillet-2016.mp4");
        assertThat(downloadingItem.getUserAgent()).isEqualToIgnoringCase("AppleCoreMedia/1.0.0.10B400 (iPod; U; CPU OS 6_1_5 like Mac OS X; fr_fr)");
    }


    @Test
    @SuppressWarnings("unchecked")
    public void should_get_url_with_short_id() throws IOException, URISyntaxException, UnirestException {
        /* Given */
        String url = "http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.m3u8?vk=MTMzMTU2NDUubTN1OA==&st=UycCudlvBB6aTcCG37_Ulw&e=1492276114&t=1492265314";

        // Fetch HTML
        Item item = Item.builder().url("http://www.tf1.fr/tf1/19h-live/videos/19h-live-20-juillet-2016.html").build();
        when(htmlService.get(eq(item.getUrl()))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/tf1replay/19h-live.short-id.item.html"));

        // Fetch WAT WEB_HTML
        GetRequest apiRequest = mock(GetRequest.class);
        HttpResponse apiResponse = mock(HttpResponse.class);
        when(urlService.get("http://www.wat.tv/get/webhtml/3184238")).thenReturn(apiRequest);
        when(apiRequest.header(any(), any())).thenReturn(apiRequest);
        when(apiRequest.asString()).thenReturn(apiResponse);
        when(apiResponse.getBody()).thenReturn("{\n" +
                "  \"hls\": \"http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.m3u8?vk=MTMzMTU2NDUubTN1OA==&st=UycCudlvBB6aTcCG37_Ulw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001\",\n" +
                "  \"mpd\": \"http://das-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.mpd?vk=MTMzMTU2NDUubXBk&st=SFYRoQwZsQ-84qF0vnX6Sw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001\"\n" +
                "}");

        GetRequest m3u8request = mock(GetRequest.class, RETURNS_DEEP_STUBS);
        HttpResponse m3u8Response = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        when(urlService.get(url)).thenReturn(m3u8request);
        when(m3u8request.header(anyString(), anyString())).thenReturn(m3u8request);
        when(m3u8request.asString()).thenReturn(m3u8Response);
        when(m3u8Response.getRawBody()).thenReturn(new NullInputStream(1L));

        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgument(0)));
        when(urlService.getRealURL(anyString(), any(Consumer.class))).thenReturn(url);
        when(m3U8Service.findBestQuality(any(InputStream.class))).thenReturn(Option("foo/bar/video.mp4"));
        when(urlService.addDomainIfRelative(anyString(), anyString())).thenCallRealMethod();

        /* When */
        DownloadingItem itemUrl = extractor.extract(item);

        /* Then */
        assertThat(itemUrl.url()).contains("http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/foo/bar/video.mp4");
    }

    @Test
    public void should_be_compatible() {
        /* Given */
        String url = "www.tf1.fr/tf1/19h-live/videos";
        /* When */
        Integer compatibility = extractor.compatibility(url);
        /* Then */
        assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        /* Given */
        String url = "www.tf1.com/foo/bar/videos";
        /* When */
        Integer compatibility = extractor.compatibility(url);
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

//    @Test
//    public void should_have_pojo_for_response() throws IOException, URISyntaxException {
//        /* Given */
//        /*
//            Due to limitation in fluent API / Mocking, I can't reach the serialization of data from text to POJO, so
//            I have to write test to check it independently
//         */
//        String json = "{\n" +
//                "  \"hls\": \"http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.m3u8?vk=MTMzMTU2NDUubTN1OA==&st=UycCudlvBB6aTcCG37_Ulw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001\",\n" +
//                "  \"mpd\": \"http://das-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.mpd?vk=MTMzMTU2NDUubXBk&st=SFYRoQwZsQ-84qF0vnX6Sw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001\"\n" +
//                "}";
//
//        /* When */
//        TF1ReplayVideoUrl videoUrl = IOUtils.stringAsJson(json).read("$", TF1ReplayVideoUrl.class);
//
//        /* Then */
//        assertThat(videoUrl).isNotNull();
//        assertThat(videoUrl.getHls()).isEqualTo("http://ios-q1.tf1.fr/2/USP-0x0/56/45/13315645/ssm/13315645.ism/13315645.m3u8?vk=MTMzMTU2NDUubTN1OA==&st=UycCudlvBB6aTcCG37_Ulw&e=1492276114&t=1492265314&min_bitrate=100000&max_bitrate=1600001");
//    }
}