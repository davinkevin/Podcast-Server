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
                .title("Des esquimaux au soleil")
                .url("http://www.6play.fr/les-p-tits-cuistots-p_5190/Des-esquimaux-au-soleil-c_11506160")
                .build()
        );
    }

    @Test
    public void should_get_url_for_m6_item() throws IOException, URISyntaxException {
        /* GIVEN */
        when(htmlService.get(downloader.getItem().getUrl())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/Des-esquimaux-au-soleil-c_11506160.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        /* WHEN  */
        String url = downloader.getItemUrl(downloader.getItem());
        /* THEN  */
        assertThat(url).isEqualToIgnoringCase("http://lb.cdn.m6web.fr/s/cgd/5/dabc9aca36118e0c5e029f4ef690afb4/58d20ccb/QUR8RlJ8R1B8R0Z8TVF8WVR8TUN8TkN8UEZ8UkV8Qkx8TUZ8UE18VEZ8V0Y%3D/usp/mb_sd3/a/b/6/Les-P-tits-cuistots_c11506160_Des-esquimaux-a/Les-P-tits-cuistots_c11506160_Des-esquimaux-a_unpnp.ism/Manifest.m3u8");
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