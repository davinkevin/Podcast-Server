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
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 22/03/2017 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class SixPlayDownloaderTest {

    private @Mock JsonService jsonService;
    private @Mock HtmlService htmlService;
/*    private @Mock ProcessService processService;
    private @Mock FfmpegService ffmpegService;
    private @Mock M3U8Service m3U8Service;
    private @Mock UrlService urlService;
    private @Mock MimeTypeService mimeTypeService;
    private @Mock ItemRepository itemRepository;
    private @Mock PodcastRepository podcastRepository;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SimpMessagingTemplate template;*/
    private @InjectMocks SixPlayDownloader downloader;

    @Before
    public void beforeEach() {
        downloader.setItem(Item.builder()
                .title("Les salariés de Whirlpool peuvent compter sur le soutien de Madénian et VDB")
                .url("http://www.6play.fr/le-message-de-madenian-et-vdb-p_6730/mm-vdb-28-04-c_11681670")
                .build()
        );
    }

    @Test
    public void should_get_url_for_m6_item() throws IOException, URISyntaxException {
        /* GIVEN */
        when(htmlService.get(downloader.getItem().getUrl())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-28-04-c_11681670.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        /* WHEN  */
        String url = downloader.getItemUrl(downloader.getItem());
        /* THEN  */
        assertThat(url).isEqualToIgnoringCase("http://lb.cdn.m6web.fr/s/cd/5/7e27d94dce21ef059e1a5ae389e5cafb/59063acb/usp/mb_sd3/6/f/e/Le-Message-de-Maden_c11681670_Les-salaries-de/Le-Message-de-Maden_c11681670_Les-salaries-de_unpnp.ism/Manifest.m3u8");
    }

    @Test
    public void should_not_handle_url_if_not_current_item() {
        assertThat(downloader.getItemUrl(Item.builder().url("foo").build()))
                .isEqualTo("foo");
    }

    @Test
    public void should_be_only_compatible_with_6play_url() {
        assertThat(downloader.compatibility(null)).isGreaterThan(1);
        assertThat(downloader.compatibility("foo")).isGreaterThan(1);
        assertThat(downloader.compatibility("http://www.6play.fr/test")).isEqualTo(1);
    }

}
