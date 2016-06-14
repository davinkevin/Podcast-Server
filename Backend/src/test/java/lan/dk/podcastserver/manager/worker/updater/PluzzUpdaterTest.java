package lan.dk.podcastserver.manager.worker.updater;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 10/10/2015 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class PluzzUpdaterTest {

    private static final String PLUZZ_URL = "http://pluzz.francetv.fr/videos/comment_ca_va_bien.html";
    private static final Podcast PODCAST = Podcast.builder().url(PLUZZ_URL).build();
    private static final ParseContext PARSER = JsonPath.using(Configuration.builder().mappingProvider(new JacksonMappingProvider()).build());

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @Mock UrlService urlService;
    @Mock JsonService jsonService;
    @InjectMocks PluzzUpdater pluzzUpdater;

    @Before
    public void beforeEach() {
        when(urlService.newURL(anyString())).then(i -> Optional.of(new URL(((String) i.getArguments()[0]))));
    }

    @Test
    public void should_sign_the_podcast() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(PLUZZ_URL))).thenReturn(Optional.of(parse("/remote/podcast/pluzz.commentcavabien.html")));
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
        when(htmlService.get(eq(PLUZZ_URL))).thenReturn(Optional.empty());

        /* When */
        String signature = pluzzUpdater.signatureOf(PODCAST);

        /* Then */
        assertThat(signature).isEmpty();
    }

    @Test
    public void should_get_items() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(PLUZZ_URL))).thenReturn(Optional.of(parse("/remote/podcast/pluzz.commentcavabien.html")));
        when(jsonService.parse(any(URL.class))).then(i -> {
            URL url = URL.class.cast(i.getArguments()[0]);
            if (url.toString().contains("129003962"))
                return Optional.empty();
            return Optional.of(loadEpisode(StringUtils.substringBetween(url.toString(), "?idDiffusion=", "&catalogue")));
        });
        when(urlService.getM3U8UrlFormMultiStreamFile(any())).then(i -> "/fake/url" + UUID.randomUUID());

        /* When */
        Set<Item> items = pluzzUpdater.getItems(PODCAST);

        /* Then */
        assertThat(items).hasSize(5);
    }

    @Test
    public void should_get_empty_list_of_item_if_connection_failed() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(PLUZZ_URL))).thenReturn(Optional.empty());

        /* When */
        Set<Item> items = pluzzUpdater.getItems(PODCAST);

        /* Then */
        assertThat(items).isEmpty();
    }

    @Test
    public void should_get_items_if_no_played_item() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(eq(PLUZZ_URL))).thenReturn(Optional.of(parse("/remote/podcast/pluzz.commentcavabien.noplayeditem.html")));
        when(jsonService.parse(any(URL.class))).then(i -> Optional.of(loadEpisode(StringUtils.substringBetween(URL.class.cast(i.getArguments()[0]).toString(), "?idDiffusion=", "&catalogue"))));
        when(urlService.getM3U8UrlFormMultiStreamFile(any())).then(i -> "/fake/url" + UUID.randomUUID());

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

    private DocumentContext loadEpisode(String id) throws URISyntaxException, IOException {
        return PARSER.parse(Paths.get(PluzzUpdaterTest.class.getResource(String.format("/remote/podcast/pluzz/pluzz.commentcavabien.%s.json", id)).toURI()).toFile());
    }

    private Document parse(String file) throws URISyntaxException, IOException {
        return Jsoup.parse(Paths.get(PluzzUpdaterTest.class.getResource(file).toURI()).toFile(), "UTF-8");
    }
}