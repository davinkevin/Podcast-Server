package lan.dk.podcastserver.service;

import lan.dk.podcastserver.manager.worker.updater.DailymotionUpdaterTest;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 22/02/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonServiceTest {

    @Mock UrlService urlService;
    @InjectMocks JsonService jsonService;

    @Test
    public void should_read_json_from_files() throws URISyntaxException, IOException {
        /* Given */
        when(urlService.urlAsReader(any(URL.class))).thenReturn(Files.newBufferedReader(Paths.get(DailymotionUpdaterTest.class.getResource("/remote/downloader/dailymotion/user.karimdebbache.json").toURI())));

        /* When */
        Optional<JSONObject> aFakeUrl = jsonService.from(new URL("http://foo.com/"));

        /* Then */
        assertThat(aFakeUrl).isPresent();
    }

    @Test
    public void should_return_empty_if_error_during_parsing() throws IOException {
        /* Given */
        doThrow(IOException.class).when(urlService).urlAsReader(any(URL.class));

        /* When */
        Optional<JSONObject> aFakeUrl = jsonService.from(new URL("http://foo.com/"));

        /* Then */
        assertThat(aFakeUrl).isEmpty();
    }
    
    @Test
    public void should_parse_from_string() {
        /* Given */
        String object = "{}";
        /* When */
        Optional<JSONObject> parse = jsonService.from(object);

        /* Then */
        assertThat(parse).isPresent();
    }

    @Test
    public void should_return_empty_if_error_during_parsing_string() {
        /* Given */
        String object = "}{{{";

        /* When */
        Optional<JSONObject> parse = jsonService.from(object);

        /* Then */
        assertThat(parse).isEmpty();
    }
}