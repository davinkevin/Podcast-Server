package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.utils.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static lan.dk.podcastserver.entity.PodcastAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 26/03/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class SixPlayFinderTest {

    private @Mock HtmlService htmlService;
    private @Mock ImageService imageService;
    private @Mock JsonService jsonService;
    private @InjectMocks SixPlayFinder finder;

    @Test
    public void should_find_podcast() throws IOException, URISyntaxException {
        /* GIVEN */
        String url = "http://www.6play.fr/turbo-p_884";
        when(htmlService.get(anyString())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/turbo-p_884.html"));
        when(jsonService.parse(anyString())).then(i -> IOUtils.stringAsJson(i.getArgumentAt(0, String.class)));
        when(imageService.getCoverFromURL(anyString())).thenReturn(Cover.DEFAULT_COVER);
        /* WHEN  */
        Podcast podcast = finder.find(url);
        System.out.println(podcast.getDescription());
        /* THEN  */
        assertThat(podcast)
                .hasTitle("Turbo")
                .hasDescription("Magazine d'actualité automobile présenté par Dominique Chapatte.")
                .hasType("SixPlay");
        verify(imageService, times(1)).getCoverFromURL(anyString());
    }

    @Test
    public void should_be_only_compatible_with_6play_url() {
        Assertions.assertThat(finder.compatibility(null)).isGreaterThan(1);
        Assertions.assertThat(finder.compatibility("foo")).isGreaterThan(1);
        Assertions.assertThat(finder.compatibility("http://www.6play.fr/test")).isEqualTo(1);
    }
}
