package lan.dk.podcastserver.manager.worker.updater;

import javaslang.collection.Set;
import javaslang.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 10/10/2015 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class PluzzUpdaterTest {

    private static final String PLUZZ_URL = "http://pluzz.francetv.fr/videos/comment_ca_va_bien.html";
    private static final Podcast PODCAST = Podcast.builder().url(PLUZZ_URL).build();

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @Mock JsonService jsonService;
    @Mock M3U8Service m3U8Service;
    @InjectMocks PluzzUpdater pluzzUpdater;

    @Test
    public void should_sign_the_podcast() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(PLUZZ_URL))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/pluzz.commentcavabien.html"));
        when(signatureService.generateMD5Signature(anyString())).thenReturn("1234567889azerty");

        /* When */
        String signature = pluzzUpdater.signatureOf(PODCAST);

        /* Then */
        assertThat(signature)
                .isNotEmpty()
                .isEqualTo("1234567889azerty");
    }

    @Test
    public void should_return_empty_string_if_signature_fails() throws IOException {
         /* Given */
        when(htmlService.get(eq(PLUZZ_URL))).thenReturn(Option.none());

        /* When */
        String signature = pluzzUpdater.signatureOf(PODCAST);

        /* Then */
        assertThat(signature).isEmpty();
    }

    @Test
    public void should_get_items() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(PLUZZ_URL))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/pluzz.commentcavabien.html"));
        when(jsonService.parseUrl(anyString())).then(i -> {
            String url = i.getArgumentAt(0, String.class);
            return url.contains("129003962") ? Option.none() : IOUtils.fileAsJson(asResourcePath(getIdFromUrl(url)));
        });
        when(m3U8Service.getM3U8UrlFormMultiStreamFile(anyString())).then(i -> "/fake/url/" + UUID.randomUUID());

        /* When */
        Set<Item> items = pluzzUpdater.getItems(PODCAST);

        /* Then */
        assertThat(items).hasSize(5);
    }

    @Test
    public void should_get_empty_list_of_item_if_connection_failed() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(PLUZZ_URL))).thenReturn(Option.none());

        /* When */
        Set<Item> items = pluzzUpdater.getItems(PODCAST);

        /* Then */
        assertThat(items).isEmpty();
    }

    @Test
    public void should_get_items_if_no_played_item() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(PLUZZ_URL))).thenReturn(IOUtils.fileAsHtml("/remote/podcast/pluzz.commentcavabien.noplayeditem.html"));
        when(jsonService.parseUrl(anyString())).then(i -> IOUtils.fileAsJson(asResourcePath(getIdFromUrl(i.getArgumentAt(0, String.class)))));
        when(m3U8Service.getM3U8UrlFormMultiStreamFile(anyString())).then(i -> "/fake/url/" + UUID.randomUUID());

        /* When */
        Set<Item> items = pluzzUpdater.getItems(PODCAST);

        /* Then */
        assertThat(items).hasSize(5);
    }

    @Test
    public void should_has_pluzz_type() {
        assertThat(pluzzUpdater.type().key()).isEqualTo("Pluzz");
        assertThat(pluzzUpdater.type().name()).isEqualTo("Pluzz");
    }

    private String getIdFromUrl(String url) {
        return StringUtils.substringBetween(url, "?idDiffusion=", "&catalogue");
    }

    private String asResourcePath(String id) {
        return String.format("/remote/podcast/pluzz/pluzz.commentcavabien.%s.json", id);
    }
}