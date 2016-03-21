package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.PodcastAssert;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class CanalPlusFinderTest {

    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @InjectMocks CanalPlusFinder canalPlusFinder;

    @Test
    public void should_find_podcast() throws IOException, URISyntaxException {
        /* Given */
        Cover cover = new Cover("http://media.canal-plus.com/image/81/7/642817.ogfb.jpg", 200, 200);
        when(imageService.getCoverFromURL(eq("http://media.canal-plus.com/image/81/7/642817.ogfb.jpg"))).thenReturn(cover);
        when(htmlService.get(eq("http://www.canalplus.fr/c-emissions/pid6378-c-le-petit-journal.html"))).thenReturn(readFile("/remote/podcast/canalplus/lepetitjournal.html"));

        /* When */
        Podcast podcast = canalPlusFinder.find("http://www.canalplus.fr/c-emissions/pid6378-c-le-petit-journal.html");

        /* Then */
        PodcastAssert
                .assertThat(podcast)
                .hasUrl("http://www.canalplus.fr/c-emissions/c-le-petit-journal/pid6515-le-petit-journal.html")
                .hasTitle("Le Petit Journal")
                .hasType("CanalPlus")
                .hasCover(cover)
                .hasDescription("Yann Barthes présente le petit journal à 20h10 -  CANALPLUS.FR");
    }

    @Test
    public void should_be_compatible() {
        assertThat(canalPlusFinder.compatibility("http://www.canalplus.fr/c-emissions/pid6378-c-le-petit-journal.html")).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(canalPlusFinder.compatibility("http://www.foo.fr/bar/to.html")).isGreaterThan(1);
    }

    public static Optional<Document> readFile(String uri) throws URISyntaxException, IOException {
        return Optional.of(Jsoup.parse(Paths.get(YoutubeFinderTest.class.getResource(uri).toURI()).toFile(),"UTF-8"));
    }

}