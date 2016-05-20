package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.PodcastAssert;
import lan.dk.podcastserver.service.HtmlService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;

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
    @InjectMocks JeuxVideoComFinder jeuxVideoComFinder;

    @Test
    public void should_find_podcast() throws IOException, URISyntaxException {
        /* Given */
        String url = "/remote/podcast/JeuxVideoCom/chroniques-video.htm";
        when(htmlService.get(eq(url))).thenReturn(readFile("/remote/podcast/JeuxVideoCom/chroniques-video.htm"));

        /* When */
        Podcast podcast = jeuxVideoComFinder.find(url);

        /* Then */
        PodcastAssert
                .assertThat(podcast)
                .hasTitle("Dernières vidéos de chroniques")
                .hasDescription("Découvrez toutes les chroniques de jeux vidéo ainsi que les dernières vidéos de chroniques comme Chronique,Chronique,Chronique,...")
                .hasType("JeuxVideoCom")
                .hasUrl(url);

    }

    @Test
    public void should_not_find_data_for_this_url() {
        /* Given */
        when(htmlService.get(any())).thenReturn(Optional.empty());

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
    
    public static Optional<Document> readFile(String uri) throws URISyntaxException, IOException {
        return Optional.of(Jsoup.parse(Paths.get(JeuxVideoComFinderTest.class.getResource(uri).toURI()).toFile(),"UTF-8"));
    }
}