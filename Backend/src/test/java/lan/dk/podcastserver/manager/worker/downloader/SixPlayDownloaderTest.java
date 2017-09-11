package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.utils.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 22/03/2017 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class SixPlayDownloaderTest {

    private @Mock JsonService jsonService;
    private @Mock HtmlService htmlService;
    private @InjectMocks SixPlayDownloader downloader;

    @Before
    public void beforeEach() {
        downloader.setItem(Item.builder()
                .title("Les salariés de Whirlpool peuvent compter sur le soutien de Madénian et VDB")
                .url("http://www.6play.fr/le-message-de-madenian-et-vdb-p_6730/mm-vdb-02-06-c_11693282.html")
                .build()
        );
    }

    @Test
    public void should_get_url_for_m6_item() throws IOException, URISyntaxException {
        /* GIVEN */
        when(htmlService.get(downloader.getItem().getUrl())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-02-06-c_11693282.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        /* WHEN  */
        String url = downloader.getItemUrl(downloader.getItem());
        /* THEN  */
        assertThat(url).isEqualToIgnoringCase("http://lb.cdn.m6web.fr/s/cd/5/7b4f83770465c93e0d15b67cfdc1f6d3/5932fafb/usp/mb_sd3/c/0/2/Le-Message-de-Maden_c11693282_Episodes-du-02-/Le-Message-de-Maden_c11693282_Episodes-du-02-_unpnp.ism/Manifest.m3u8");
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

}
