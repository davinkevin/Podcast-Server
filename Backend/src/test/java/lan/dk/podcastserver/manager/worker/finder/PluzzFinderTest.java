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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class PluzzFinderTest {

    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @InjectMocks PluzzFinder pluzzFinder;

    @Test
    public void should_find_podcast() throws IOException, URISyntaxException {
        /* Given */
        Cover cover = Cover.builder().url("http://refonte.webservices.francetelevisions.fr/image/referentiel_emissions/129003962/1444405142/480/0/0/0/img.jpg").width(200).height(200).build();
        when(imageService.getCoverFromURL(eq("http://refonte.webservices.francetelevisions.fr/image/referentiel_emissions/129003962/1444405142/480/0/0/0/img.jpg")))
                .thenReturn(cover);
        when(htmlService.get(eq("http://pluzz.francetv.fr/videos/comment_ca_va_bien.html")))
                .thenReturn(IOUtils.fileAsHtml("/remote/podcast/pluzz.commentcavabien.html"));

        /* When */
        Podcast podcast = pluzzFinder.find("http://pluzz.francetv.fr/videos/comment_ca_va_bien.html");

        /* Then */
        assertThat(podcast)
            .hasUrl("http://pluzz.francetv.fr/videos/comment_ca_va_bien.html")
            .hasTitle("Comment ça va bien !")
            .hasType("Pluzz")
            .hasCover(cover)
            .hasDescription("Avec son équipe de chroniqueurs, Stéphane Bern anime un rendez-vous consacrée à la beauté, à la mode, aux tendances, au bricolage ou encore ...");
    }

}