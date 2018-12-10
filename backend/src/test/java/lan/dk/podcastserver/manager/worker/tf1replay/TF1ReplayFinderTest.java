package lan.dk.podcastserver.manager.worker.tf1replay;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 21/07/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class TF1ReplayFinderTest {

    private @Mock HtmlService htmlService;
    private @Mock ImageService imageService;
    private @InjectMocks
    TF1ReplayFinder finder;

    @Test
    public void should_fetch_from_html_page() throws IOException, URISyntaxException {
        /* Given */
        String url = "www.tf1.fr/tf1/19h-live/videos";
        when(htmlService.get(eq(url))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/tf1replay/19h-live.html"));
        when(imageService.getCoverFromURL(anyString())).then(i -> Cover.builder().url(i.getArgument(0)).build());

        /* When */
        Podcast podcast = finder.find(url);

        /* Then */
        assertThat(podcast)
                .hasTitle("Vidéos & Replay 19h live - TF1")
                .hasDescription("Tous les replays  19h live: les vidéos bonus exclusives des coulisses, des interviews de  19h live:")
                .hasUrl(url)
                .hasType("TF1Replay");

        assertThat(podcast.getCover()).hasUrl("http://photos1.tf1.fr/1920/960/1920x1080-19h-5619b8-0@1x.jpg");
    }

    @Test
    public void should_fetch_from_html_page_without_url() throws IOException, URISyntaxException {
        /* Given */
        String url = "www.tf1.fr/tf1/19h-live/videos";
        when(htmlService.get(eq(url))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/tf1replay/19h-live.withoutpicture.html"));
        when(imageService.getCoverFromURL(anyString())).then(i -> Cover.builder().url(i.getArgument(0)).build());

        /* When */
        Podcast podcast = finder.find(url);

        /* Then */
        assertThat(podcast)
                .hasTitle("Vidéos & Replay 19h live - TF1")
                .hasDescription("Tous les replays  19h live: les vidéos bonus exclusives des coulisses, des interviews de  19h live:")
                .hasUrl(url)
                .hasType("TF1Replay");

        assertThat(podcast.getCover()).hasUrl("http://photos2.tf1.fr/130/65/logo_programme-284-3955bf-0@1x.jpg");
    }

    @Test
    public void should_be_compatible() {
        /* Given */
        String url = "www.tf1.fr/tf1/19h-live/videos";
        /* When */
        Integer compatibility = finder.compatibility(url);
        /* Then */
        assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        /* Given */
        String url = "www.tf1.com/foo/bar/videos";
        /* When */
        Integer compatibility = finder.compatibility(url);
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }
}
