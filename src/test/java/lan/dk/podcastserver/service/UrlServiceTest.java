package lan.dk.podcastserver.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.Reader;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 09/07/15 for Podcast Server
 */
public class UrlServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089); // No-args constructor defaults to port 8080

    UrlService urlService = new UrlService();

    @Test
    public void should_get_buffered_reader_of_url() throws Exception {
        stubFor(get(urlEqualTo("/my/resource"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("12345678")));

        /* When */ Reader readerFromURL = urlService.getReaderFromURL("http://localhost:8089/my/resource");
        /* Then */ assertThat(((BufferedReader) readerFromURL).readLine())
                .isNotNull()
                .isEqualTo("12345678");
    }

}