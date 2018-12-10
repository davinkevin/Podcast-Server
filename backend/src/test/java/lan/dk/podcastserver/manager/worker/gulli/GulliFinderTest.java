package lan.dk.podcastserver.manager.worker.gulli;

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
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 13/10/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class GulliFinderTest {

    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @InjectMocks
    GulliFinder gulliFinder;

    @Test
    public void should_find_a_podcast_for_valid_url() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/gulli/pokemon.html"));
        when(imageService.getCoverFromURL(anyString())).then(i -> Cover.builder().url(i.getArgument(0)).height(250).width(250).build());

        /* When */
        Podcast podcast = gulliFinder.find("http://replay.gulli.fr/dessins-animes/Pokemon3");

        /* Then */
        assertThat(podcast)
                .hasUrl("http://replay.gulli.fr/dessins-animes/Pokemon3")
                .hasTitle("Pokémon")
                .hasType("Gulli");

        assertThat(podcast.getCover())
                .hasUrl("http://resize1-gulli.ladmedia.fr/r/340,255,smartcrop,center-top/img/var/storage/imports/replay/images_programme/pokemon_s19.jpg")
                .hasHeight(250)
                .hasWidth(250);
    }

    @Test
    public void should_be_compatible() {
        assertThat(gulliFinder.compatibility("http://replay.gulli.fr/dessins-animes/Pokemon3"))
                .isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(gulliFinder.compatibility("http://foo.bar.fr/dessins-animes/Pokemon3"))
                .isGreaterThan(1);
    }
}
