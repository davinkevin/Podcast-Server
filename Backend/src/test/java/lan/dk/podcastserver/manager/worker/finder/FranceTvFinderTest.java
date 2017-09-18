package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.utils.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class FranceTvFinderTest {

    private @Mock HtmlService htmlService;
    private @Mock ImageService imageService;
    private @InjectMocks FranceTvFinder franceTvFinder;

    @Test
    public void should_find_podcast() throws IOException, URISyntaxException {
        /* Given */
        Cover cover = Cover.builder()
                .url("https://www.france.tv/image/carre/265/265/y/r/v/8743665e-phpgbpvry.jpg")
                .width(200)
                .height(200)
            .build();

        when(imageService.getCoverFromURL(eq("https://www.france.tv/image/carre/265/265/y/r/v/8743665e-phpgbpvry.jpg"))).thenReturn(cover);
        when(htmlService.get(eq("https://www.france.tv/france-2/secrets-d-histoire/"))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/francetv/secrets-d-histoire.html"));

        /* When */
        Podcast podcast = franceTvFinder.find("https://www.france.tv/france-2/secrets-d-histoire/");

        /* Then */
        assertThat(podcast)
            .hasUrl("https://www.france.tv/france-2/secrets-d-histoire/")
            .hasTitle("Secrets d'Histoire")
            .hasType("FranceTv")
            .hasCover(cover)
            .hasDescription("Secrets d'Histoire est une émission de télévision présentée par Stéphane Bern. Chaque numéro retrace la vie d'un grand personnage de l'histoire et met en lumière des lieux hautement emblématiques du patrimoine.\n" +
                    " \n" +
                    "Magazine n°14\n" +
                    " \n" +
                    "Accessible à tous, le magazine Secrets d’Histoire vous entraîne au cœur des épisodes mystérieux de l’histoire à travers des reportages, des enquêtes, des quizz… et bien plus encore ! En savoir plus");
    }


    @Test
    public void should_be_compatible() {
        /* GIVEN */
        String url = "https://www.france.tv/foo/bar/toto";
        /* WHEN  */
        Integer compatibility = franceTvFinder.compatibility(url);
        /* THEN  */
        Assertions.assertThat(compatibility).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        /* GIVEN */
        String url = "https://www.france2.tv/foo/bar/toto";
        /* WHEN  */
        Integer compatibility = franceTvFinder.compatibility(url);
        /* THEN  */
        Assertions.assertThat(compatibility).isGreaterThan(1);
    }
}
