package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.utils.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
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
        Cover cover = Cover.builder().url("http://media.canal-plus.com/image/81/7/642817.ogfb.jpg").width(200).height(200).build();
        when(imageService.getCoverFromURL(eq("http://media.canal-plus.com/image/81/7/642817.ogfb.jpg"))).thenReturn(cover);
        when(htmlService.get(eq("http://www.canalplus.fr/c-emissions/pid6378-c-le-petit-journal.html"))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/canalplus/lepetitjournal.html"));

        /* When */
        Podcast podcast = canalPlusFinder.find("http://www.canalplus.fr/c-emissions/pid6378-c-le-petit-journal.html");

        /* Then */
        assertThat(podcast)
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

}