package lan.dk.podcastserver.manager.worker.jeuxvideocom;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.jeuxvideocom.JeuxVideoComFinder;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.utils.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static io.vavr.API.None;
import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 23/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class JeuxVideoComFinderTest {

    @Mock HtmlService htmlService;
    @InjectMocks
    JeuxVideoComFinder jeuxVideoComFinder;

    @Test
    public void should_find_podcast() throws IOException, URISyntaxException {
        /* Given */
        String url = "/remote/podcast/JeuxVideoCom/chroniques-video.htm";
        when(htmlService.get(eq(url))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/JeuxVideoCom/chroniques-video.htm"));

        /* When */
        Podcast podcast = jeuxVideoComFinder.find(url);

        /* Then */
        assertThat(podcast)
                .hasTitle("Dernières vidéos de chroniques")
                .hasDescription("Découvrez toutes les chroniques de jeux vidéo ainsi que les dernières vidéos de chroniques comme Chronique,Chronique,Chronique,...")
                .hasType("JeuxVideoCom")
                .hasUrl(url);

    }

    @Test
    public void should_not_find_data_for_this_url() {
        /* Given */
        when(htmlService.get(any())).thenReturn(None());

        /* When */
        Podcast podcast = jeuxVideoComFinder.find("foo/bar");

        /* Then */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST);
    }

    @Test
    public void should_be_compatible() {
        /* Given */
        String url = "www.jeuxvideo.com/foo/bar";

        /* When */
        Integer compatibility = jeuxVideoComFinder.compatibility(url);

        /* Then */
        assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        /* Given */
        String url = "www.youtube.com/foo/bar";

        /* When */
        Integer compatibility = jeuxVideoComFinder.compatibility(url);

        /* Then */
        assertThat(compatibility).isGreaterThan(1);
    }
}
