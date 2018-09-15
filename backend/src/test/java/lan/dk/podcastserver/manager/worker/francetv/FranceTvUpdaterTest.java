package lan.dk.podcastserver.manager.worker.francetv;

import com.github.davinkevin.podcastserver.service.SignatureService;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.utils.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 01/07/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class FranceTvUpdaterTest {
    private static final String FRANCETV_URL = "https://www.france.tv/france-2/secrets-d-histoire/";
    private static final Podcast PODCAST = Podcast.builder().url(FRANCETV_URL).build();
    private static final String NORMAL_SIGNATURE = "d780029b17c1fa08722c1b360af8a674";

    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SignatureService signatureService;
    private @Mock Validator validator;
    private @Mock HtmlService htmlService;
    private @Mock ImageService imageService;
    private @Mock JsonService jsonService;
    private @InjectMocks
    FranceTvUpdater franceTvUpdater;

    @Test
    public void should_sign_the_podcast() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.html")));
        when(signatureService.fromText(anyString())).thenCallRealMethod();

        /* When */
        String signature = franceTvUpdater.signatureOf(PODCAST);

        /* Then */
        assertThat(signature)
                .isNotEmpty()
                .isEqualTo(NORMAL_SIGNATURE);
    }

    @Test
    public void should_have_same_signature_even_if_date_change() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire_changed.html")));
        when(signatureService.fromText(anyString())).thenCallRealMethod();

        /* When */
        String signature = franceTvUpdater.signatureOf(PODCAST);

        /* Then */
        assertThat(signature)
                .isNotEmpty()
                .isEqualTo(NORMAL_SIGNATURE);
    }

    @Test
    public void should_have_different_sign_if_new_item_in_podcast() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire_with_new_items.html")));
        when(signatureService.fromText(anyString())).thenCallRealMethod();

        /* When */
        String signature = franceTvUpdater.signatureOf(PODCAST);

        /* Then */
        assertThat(signature)
                .isNotEmpty()
                .isEqualTo("1d45c2921ac536ab5b32e4605a76756e")
                .isNotEqualTo(NORMAL_SIGNATURE);
    }

    @Test
    public void should_get_items() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.html")));
        when(jsonService.parseUrl(anyString())).then(this::loadJsonCatalog);
        when(imageService.getCoverFromURL(anyString())).thenReturn(new Cover());

        /* When */
        Set<Item> items = franceTvUpdater.getItems(PODCAST);

        /* Then */
        assertThat(items).hasSize(8);
        items.forEach(i -> {
            assertThat(i.getTitle()).isNotEmpty();
            assertThat(i.getDescription()).isNotEmpty();
            assertThat(i.getUrl()).isNotEmpty();
            assertThat(i.getCover()).isNotNull().isNotSameAs(Cover.DEFAULT_COVER);
            assertThat(i.getPubDate()).isNotNull();
        });
    }

    @Test
    public void should_has_francetv_type() {
        assertThat(franceTvUpdater.type().key()).isEqualTo("FranceTv");
        assertThat(franceTvUpdater.type().name()).isEqualTo("France•tv");
    }

    private Object loadJsonCatalog(InvocationOnMock i) {
        String url = ((String) i.getArgument(0)).replace("https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=", "") + ".json";
        return IOUtils.fileAsJson(from(url));
    }

    private String from(String name) {
        return String.format("/remote/podcast/francetv/%s", name);
    }

}