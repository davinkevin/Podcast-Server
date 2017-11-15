package lan.dk.podcastserver.service;

import io.vavr.collection.List;
import io.vavr.control.Option;
import lan.dk.utils.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 22/07/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class M3U8ServiceTest {

    private @Mock UrlService urlService;
    private @InjectMocks M3U8Service m3U8Service;

    @Test
    public void should_select_best_audio_video_url() {
        /* Given */
        InputStream m3u8FileStream = IOUtils.fileAsStream("/remote/podcast/tf1replay/13184238.m3u8");

        /* When */
        Option<String> bestQuality = m3U8Service.findBestQuality(m3u8FileStream);

        /* Then */
        assertThat(bestQuality.toJavaOptional()).isPresent();
        assertThat(bestQuality.toJavaOptional()).hasValue("13184238-audio%3D64000-video%3D2500299.m3u8?vk=MTMxODQyMzgubTN1OA==&st=D79oBJFWiP__EA4uMJAejg&e=1469146222&t=1469135422&min_bitrate=");
    }

    @Test
    public void should_not_select_video_url() throws IOException {
        /* Given */
        InputStream is = mock(InputStream.class);
        doThrow(IOException.class).when(is).read();

        /* When */
        Option<String> bestQuality = m3U8Service.findBestQuality(is);

        /* Then */
        assertThat(bestQuality.toJavaOptional()).isEmpty();
    }

    @Test
    public void should_get_last_m3u8_url() throws IOException {
        /* Given */
        String resourcePath = "/__files/service/urlService/canalplus.lepetitjournal.20150707.m3u8";
        when(urlService.asReader(eq(resourcePath))).then(i -> IOUtils.fileAsReader(resourcePath));
        when(urlService.addDomainIfRelative(anyString(), anyString())).thenCallRealMethod();

        /* When */
        String lastUrl = m3U8Service.getM3U8UrlFormMultiStreamFile(resourcePath);

        /* Then */
        assertThat(lastUrl).isEqualTo("http://us-cplus-aka.canal-plus.com/i/1507/02/nip_NIP_59957_,200k,400k,800k,1500k,.mp4.csmil/segment146_3_av.ts");
    }

    @Test
    public void should_handle_relative_url() throws IOException {
        /* Given */
        String resourcePath = "http://a.custom.dom/__files/service/urlService/relative.m3u8";
        when(urlService.asReader(eq(resourcePath))).then(i -> IOUtils.fileAsReader("/__files/service/urlService/relative.m3u8"));
        when(urlService.addDomainIfRelative(anyString(), anyString())).thenCallRealMethod();

        /* When */  String lastUrl = m3U8Service.getM3U8UrlFormMultiStreamFile(resourcePath);
        /* Then */  assertThat(lastUrl).isEqualTo("http://a.custom.dom/__files/service/urlService/9dce76b19072beda39720aa04aa2e47a-video=1404000-audio_AACL_fra_70000_315=70000.m3u8");
    }

    @Test
    public void should_return_null_if_exception() throws IOException {
        /* Given */
        String resourcePath = "/__files/service/urlService/canalplus.lepetitjournal.20150707.m3u8";
        doThrow(IOException.class).when(urlService).asReader(eq(resourcePath));

        /* When */
        String lastUrl = m3U8Service.getM3U8UrlFormMultiStreamFile(resourcePath);

        /* Then */
        assertThat(lastUrl).isNull();
    }

    @Test
    public void should_return_null_if_url_is_null() {
        /* Given */
        /* When */
        String lastUrl = m3U8Service.getM3U8UrlFormMultiStreamFile(null);
        /* Then */
        assertThat(lastUrl).isNull();
    }
}
