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
        /* Given */
        stubFor(get(urlEqualTo("/my/resource"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("12345678")));

        /* When */
        Reader readerFromURL = urlService.getReaderFromURL("http://localhost:8089/my/resource");

        /* Then */
        assertThat(readerFromURL).isOfAnyClassIn(BufferedReader.class);
        assertThat(((BufferedReader) readerFromURL).readLine())
                .isNotNull()
                .isEqualTo("12345678");
    }

    @Test
    public void should_get_last_m3u8_url() {
        /* Given */
        stubFor(get(urlEqualTo("/my/ressources.m3u8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")
                        .withBodyFile("canalplus.lepetitjournal.20150707.m3u8")));

        /* When */  String lastUrl = urlService.getM3U8UrlFormMultiStreamFile("http://localhost:8089/my/ressources.m3u8");

        /* Then */  assertThat(lastUrl)
                .isEqualTo("http://us-cplus-aka.canal-plus.com/i/1507/02/nip_NIP_59957_,200k,400k,800k,1500k,.mp4.csmil/segment146_3_av.ts");
    }
    
    @Test
    public void should_return_null_if_exception() {
        /* Given */
        stubFor(get(urlEqualTo("/my/ressources.m3u8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")
                        .withBodyFile("canalplus.lepetitjournal.20150707.m3u8")));
        /* When */  String lastUrl = urlService.getM3U8UrlFormMultiStreamFile("http://localhost:8089/my/ressources2.m3u8");
        /* Then */  assertThat(lastUrl).isNull();
    }

    @Test
    public void should_return_null_if_url_is_null() {
        /* Given */ String url = null;
        /* When */  String lastUrl = urlService.getM3U8UrlFormMultiStreamFile(null);
        /* Then */  assertThat(lastUrl).isNull();
    }

    @Test
    public void should_get_real_url_after_redirection() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", "http://localhost:8089/my/ressources2.m3u8");
        doRedirection("/my/ressources2.m3u8", "http://localhost:8089/my/ressources3.m3u8");
        stubFor(get(urlEqualTo("/my/ressources3.m3u8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")));

        /* When */  String lastUrl = urlService.getRealURL("http://localhost:8089/my/ressources1.m3u8");
        /* Then */  assertThat(lastUrl).isEqualTo("http://localhost:8089/my/ressources3.m3u8");
    }

    @Test(expected = RuntimeException.class)
    public void should_recject_after_too_many_redirection() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", "http://localhost:8089/my/ressources2.m3u8");
        doRedirection("/my/ressources2.m3u8", "http://localhost:8089/my/ressources3.m3u8");
        doRedirection("/my/ressources3.m3u8", "http://localhost:8089/my/ressources4.m3u8");
        doRedirection("/my/ressources4.m3u8", "http://localhost:8089/my/ressources5.m3u8");
        doRedirection("/my/ressources5.m3u8", "http://localhost:8089/my/ressources6.m3u8");
        doRedirection("/my/ressources6.m3u8", "http://localhost:8089/my/ressources7.m3u8");
        doRedirection("/my/ressources7.m3u8", "http://localhost:8089/my/ressources8.m3u8");
        doRedirection("/my/ressources8.m3u8", "http://localhost:8089/my/ressources9.m3u8");
        doRedirection("/my/ressources9.m3u8", "http://localhost:8089/my/ressources10.m3u8");
        doRedirection("/my/ressources10.m3u8", "http://localhost:8089/my/ressources11.m3u8");

        /* When */ urlService.getRealURL("http://localhost:8089/my/ressources1.m3u8");
    }

    @Test
    public void should_treat_non_reachable_url() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", "http://localhost:1234/my/ressources2.m3u8");

        /* When */  String lastUrl = urlService.getRealURL("http://localhost:8089/my/ressources1.m3u8");
        /* Then */  assertThat(lastUrl).isEqualTo("http://localhost:1234/my/ressources2.m3u8");
    }

    private void doRedirection(String mockUrl, String redirectionUrl) {
        stubFor(get(urlEqualTo(mockUrl))
                .willReturn(aResponse()
                        .withStatus(301)
                        .withHeader("Location", redirectionUrl)));
    }

}