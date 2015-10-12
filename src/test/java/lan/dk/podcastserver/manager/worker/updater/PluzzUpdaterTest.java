package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
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
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
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

    private Document parse(String file) throws URISyntaxException, IOException {
        return Jsoup.parse(Paths.get(PluzzUpdaterTest.class.getResource(file).toURI()).toFile(), "UTF-8" );
    }



}