package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 10/10/2015 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class PluzzUpdaterTest {

    public static final String PLUZZ_URL = "http://pluzz.francetv.fr/videos/comment_ca_va_bien.html";

    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @Mock UrlService urlService;
    @InjectMocks PluzzUpdater pluzzUpdater;
    public static final Podcast PODCAST = Podcast.builder().url(PLUZZ_URL).build();

    @Test
    public void should_sign_the_podcast() throws IOException, URISyntaxException {
        /* Given */
        Connection connection = mock(Connection.class);
        Connection.Response response = mock(Connection.Response.class);
        when(htmlService.connectWithDefault(PLUZZ_URL)).thenReturn(connection);
        when(connection.execute()).thenReturn(response);
        when(response.parse()).thenReturn(parse("/remote/podcast/pluzz.commentcavabien.html"));
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
        Connection connection = mock(Connection.class);
        when(htmlService.connectWithDefault(PLUZZ_URL)).thenReturn(connection);
        doThrow(IOException.class).when(connection).execute();

        /* When */
        String signature = pluzzUpdater.signatureOf(PODCAST);

        /* Then */
        assertThat(signature)
                .isEmpty();
    }

    @Test
    public void should_get_items() throws IOException, URISyntaxException {
        /* Given */
        Connection connection = mock(Connection.class);
        Connection.Response response = mock(Connection.Response.class);
        when(htmlService.connectWithDefault(PLUZZ_URL)).thenReturn(connection);
        when(connection.execute()).thenReturn(response);
        when(response.parse()).thenReturn(parse("/remote/podcast/pluzz.commentcavabien.html"));
        doThrow(IOException.class).when(urlService).getReaderFromURL(contains("129003962"));
        when(urlService.getReaderFromURL(not(contains("129003962")))).then(invocation -> loadEpisode(StringUtils.substringBetween(String.valueOf(invocation.getArguments()[0]), "?idDiffusion=", "&catalogue")));

        /* When */
        Set<Item> items = pluzzUpdater.getItems(PODCAST);

        /* Then */
        assertThat(items)
                .hasSize(5);
    }

    @Test
    public void should_get_empty_list_of_item_if_connection_failed() throws IOException, URISyntaxException {
        /* Given */
        Connection connection = mock(Connection.class);
        when(htmlService.connectWithDefault(PLUZZ_URL)).thenReturn(connection);
        doThrow(IOException.class).when(connection).execute();

        /* When */
        Set<Item> items = pluzzUpdater.getItems(PODCAST);

        /* Then */
        assertThat(items)
                .isEmpty();
    }

    private Reader loadEpisode(String id) throws URISyntaxException, IOException {
        return Files.newBufferedReader(Paths.get(PluzzUpdaterTest.class.getResource(String.format("/remote/podcast/pluzz/pluzz.commentcavabien.%s.json", id)).toURI()));
    }

    private Document parse(String file) throws URISyntaxException, IOException {
        return Jsoup.parse(Paths.get(PluzzUpdaterTest.class.getResource(file).toURI()).toFile(), "UTF-8");
    }
}