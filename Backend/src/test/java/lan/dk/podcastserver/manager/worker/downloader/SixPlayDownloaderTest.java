package lan.dk.podcastserver.manager.worker.downloader;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.utils.IOUtils;
import org.apache.commons.io.input.NullInputStream;
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
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static io.vavr.API.Option;
import static io.vavr.API.Try;
import static io.vavr.API.println;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 22/03/2017 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class SixPlayDownloaderTest {

    private @Mock M3U8Service m3U8Service;
    private @Mock UrlService urlService;
    private @Mock SimpMessagingTemplate template;
    private @Mock ItemDownloadManager itemDownloadManager;
    private @Mock ProcessService processService;
    private @Mock FfmpegService ffmpegService;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock JsonService jsonService;
    private @Mock HtmlService htmlService;
    private @InjectMocks SixPlayDownloader downloader;

    @Before
    public void beforeEach() {
        downloader.setItem(Item.builder()
                .title("Les salariés de Whirlpool peuvent compter sur le soutien de Madénian et VDB")
                .url("http://www.6play.fr/le-message-de-madenian-et-vdb-p_6730/mm-vdb-02-06-c_11693282.html")
                .podcast(Podcast.builder().title("M6Podcast").build())
                .status(Status.STARTED)
                .build()
        );
        downloader.setItemDownloadManager(itemDownloadManager);
    }

    @Test
    public void should_get_url_for_m6_item() throws IOException, URISyntaxException {
        /* GIVEN */
        when(htmlService.get(downloader.getItem().getUrl())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-02-06-c_11693282.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        /* WHEN  */
        String url = downloader.getItemUrl(downloader.getItem());
        /* THEN  */
        assertThat(url).isEqualToIgnoringCase("https://lb.cdn.m6web.fr/s/sd/5/05512e60d213fc7aecc4ecdd9990547d/59a76336/usp/mb_sd3/c/0/2/Le-Message-de-Maden_c11693282_Episodes-du-02-/Le-Message-de-Maden_c11693282_Episodes-du-02-_unpnp.ism/Manifest.m3u8");
        verify(jsonService, times(1)).parse(anyString());
        verify(htmlService, times(1)).get(anyString());
    }

    @Test
    public void should_do_the_computation_only_once() throws IOException, URISyntaxException {
        /* GIVEN */
        when(htmlService.get(downloader.getItem().getUrl())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-02-06-c_11693282.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));

        /* WHEN  */
        String url = downloader.getItemUrl(downloader.getItem());
        String secondUrl = downloader.getItemUrl(downloader.getItem());

        /* THEN  */
        assertThat(url).isSameAs(secondUrl);
        verify(jsonService, times(1)).parse(anyString());
        verify(htmlService, times(1)).get(anyString());
    }

    @Test
    public void should_be_only_compatible_with_6play_url() {
        assertThat(downloader.compatibility(null)).isGreaterThan(1);
        assertThat(downloader.compatibility("foo")).isGreaterThan(1);
        assertThat(downloader.compatibility("http://www.6play.fr/test")).isEqualTo(1);
    }

    @Test
    public void should_transform_title() {
        /* GIVEN */
        Item item = Item.builder().url("http://www.6play.fr/le-message-de-madenian-et-vdb-p_6730/mm-vdb-02-06-c_11693282.html?foo=bar").build();

        /* WHEN  */
        String fileName = downloader.getFileName(item);

        /* THEN  */
        assertThat(fileName).isEqualToIgnoringCase("mm-vdb-02-06-c_11693282.mp4");
    }

    @Test
    public void should_return_an_empty_string_if_url_null() {
        /* GIVEN */
        /* WHEN  */
        String fileName = downloader.getFileName(Item.DEFAULT_ITEM);

        /* THEN  */
        assertThat(fileName).isEqualToIgnoringCase("");
    }

    @Test
    public void should_call_m3u8_downloader_if_only_one_url() throws IOException, URISyntaxException {
        /* GIVEN */
        Process process = mock(Process.class);
        when(htmlService.get(downloader.getItem().getUrl())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-02-06-c_11693282.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        when(podcastServerParameters.getRootfolder()).thenReturn(IOUtils.ROOT_TEST_PATH);
        when(ffmpegService.getDurationOf(anyString(), anyString())).thenReturn(1000d);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(process);
        when(processService.waitFor(process)).thenReturn(Try(() -> 1));

        /* WHEN  */
        Item itemDownloader = downloader.download();

        /* THEN  */
        verify(jsonService, times(1)).parse(anyString());
        verify(htmlService, times(1)).get(anyString());
        verify(ffmpegService, times(1)).download(anyString(), any(), any());
    }

    @Test
    public void should_do_multiple_download_if_multiple_url() throws IOException, URISyntaxException {
        /* GIVEN */
        Process process = mock(Process.class);
        when(htmlService.get(downloader.getItem().getUrl())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/best-of-ca-va-etre-leur-fete--p_2352.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        when(podcastServerParameters.getRootfolder()).thenReturn(IOUtils.ROOT_TEST_PATH);
        when(ffmpegService.getDurationOf(anyString(), anyString())).thenReturn(1000d);
        when(ffmpegService.download(anyString(), any(), any())).thenReturn(process);
        when(processService.waitFor(process)).thenReturn(Try(() -> 1));

        /* WHEN  */
        Item itemDownloader = downloader.download();

        /* THEN  */
        verify(jsonService, times(1)).parse(anyString());
        verify(htmlService, times(1)).get(anyString());
        verify(ffmpegService, times(21)).download(anyString(), any(), any());
        verify(ffmpegService, times(42)).getDurationOf(anyString(), any());
        verify(ffmpegService, times(1)).concat(any(), anyVararg());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_transform_url_if_usp_hls_h264() throws IOException, URISyntaxException, UnirestException {
        /* GIVEN */
        when(htmlService.get(downloader.getItem().getUrl())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/Episodes-du-29-aout-c_11743954.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));

        // Fetch WAT WEB_HTML
        GetRequest m3u8request = mock(GetRequest.class, RETURNS_DEEP_STUBS);
        HttpResponse m3u8Response = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        when(urlService.get("https://lb.cdn.m6web.fr/s/sd/5/6eb6bccfb3a71525ecac8b6da03c9e4a/59a770c8/usp/mb_sd3/e/0/0/Scenes-de-menages_c11743954_Episodes-du-29-ao/Scenes-de-menages_c11743954_Episodes-du-29-ao_unpnp.ism/Scenes-de-menages_c11743954_Episodes-du-29-ao_unpnp.m3u8")).thenReturn(m3u8request);
        when(m3u8request.header(anyString(), anyString())).thenReturn(m3u8request);
        when(m3u8request.asString()).thenReturn(m3u8Response);
        when(m3u8Response.getRawBody()).thenReturn(new NullInputStream(1L));
        when(m3U8Service.findBestQuality(any(InputStream.class))).thenReturn(Option("Scenes-de-menages_c11743954_Episodes-du-29-ao_unpnp-audio1=93468-sd3=1501000.m3u8"));
        when(urlService.addDomainIfRelative(anyString(), anyString())).thenCallRealMethod();

        /* WHEN  */
        String url = downloader.getItemUrl(downloader.getItem());

        /* THEN  */
        assertThat(url).isEqualToIgnoringCase("https://lb.cdn.m6web.fr/s/sd/5/6eb6bccfb3a71525ecac8b6da03c9e4a/59a770c8/usp/mb_sd3/e/0/0/Scenes-de-menages_c11743954_Episodes-du-29-ao/Scenes-de-menages_c11743954_Episodes-du-29-ao_unpnp.ism/Scenes-de-menages_c11743954_Episodes-du-29-ao_unpnp-audio1=93468-sd3=1501000.m3u8");
        verify(urlService, times(1)).get("https://lb.cdn.m6web.fr/s/sd/5/6eb6bccfb3a71525ecac8b6da03c9e4a/59a770c8/usp/mb_sd3/e/0/0/Scenes-de-menages_c11743954_Episodes-du-29-ao/Scenes-de-menages_c11743954_Episodes-du-29-ao_unpnp.ism/Scenes-de-menages_c11743954_Episodes-du-29-ao_unpnp.m3u8");
        verify(jsonService, times(1)).parse(anyString());
        verify(htmlService, times(1)).get(anyString());
    }

}
