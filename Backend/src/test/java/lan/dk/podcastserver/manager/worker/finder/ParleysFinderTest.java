package lan.dk.podcastserver.manager.worker.finder;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.PodcastAssert;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.utils.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Optional;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static lan.dk.podcastserver.entity.Podcast.DEFAULT_PODCAST;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 12/06/2016 for PodcastServer
 */
@RunWith(MockitoJUnitRunner.class)
public class ParleysFinderTest {

    private static final ParseContext PARSER = JsonPath.using(Configuration.builder().mappingProvider(new JacksonMappingProvider()).build());

    @Mock JsonService jsonService;
    @Mock ImageService imageService;
    @InjectMocks ParleysFinder parleysFinder;

    @Test
    public void should_find_podcast() throws MalformedURLException {
        /* Given */
        String originalUrl = "https://www.parleys.com/channel/devoxx-france-2015";
        String devoxx = "https://api.parleys.com/api/channel.json/devoxx-france-2015";
        Cover cover = Cover.builder()
                    .url("https://cdn.parleys.com/c/551e6811e4b06d4cf459322d/MNjgARLMlrW_banniere_carre_devoxx_fr_2015_256_256.png")
                    .height(1200)
                    .width(1600)
                .build();

        when(jsonService.parseUrl(devoxx)).then(i -> IOUtils.fileAsJson("/remote/podcast/parleys/devoxx-france-2015.details.json"));
        when(imageService.getCoverFromURL(cover.getUrl())).thenReturn(cover);

        /* When */
        Podcast podcast = parleysFinder.find(originalUrl);

        /* Then */
        PodcastAssert
                .assertThat(podcast)
                    .hasTitle("Devoxx France 2015")
                    .hasUrl(originalUrl)
                    .hasDescription("<div style=\"text-align: justify; \">Devoxx France 2015, 4ème édition, a accueilli 2500 personnes pendant 3 jours. Autour des 20 ans du langage Java et du Futur,&nbsp;<span style=\"line-height: 1.42857143;\">le programme propose plus de 150 conférences. Rendez-vous dès décembre pour s’inscrire à la prochaine édition.</span></div>")
                    .hasType("Parleys")
                    .hasCover(cover);
    }

    @Test
    public void should_return_default_if_no_id_found() {
        /* Given */
        /* When */
        Podcast podcast = parleysFinder.find("http://foo.bar.com/id/elem");

        /* Then */
        assertThat(podcast).isSameAs(DEFAULT_PODCAST);
    }

    @Test
    public void should_not_be_compatible() {
        /* Given */
        String url = "https://foo.bar.com/";
        /* When */
        Integer compatibility = parleysFinder.compatibility(url);
        /* Then */
        Assertions.assertThat(compatibility).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void should_be_compatible() {
        /* Given */
        String url = "https://parleys.com/foo/bar";
        /* When */
        Integer compatibility = parleysFinder.compatibility(url);
        /* Then */
        Assertions.assertThat(compatibility).isEqualTo(1);
    }

    private Answer<Optional<DocumentContext>> readFrom(String url) {
        return i -> Optional.of(PARSER.parse(Paths.get(ParleysFinderTest.class.getResource("/remote/podcast/parleys/" + url ).toURI()).toFile()));
    }
}