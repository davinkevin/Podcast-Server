package lan.dk.podcastserver.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URLConnection;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 09/07/15 for Podcast Server
 */
public class UrlServiceTest {

    private static final int PORT = 8089;
    private static final String LOCALHOST_WITHOUT_PORT = "http://localhost:";
    private static final String HTTP_LOCALHOST = LOCALHOST_WITHOUT_PORT + PORT;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT); // No-args constructor defaults to port 8080

    UrlService urlService = new UrlService();

    @Test
    public void should_get_buffered_reader_of_url() throws Exception {
        /* Given */
        String resource_path = "/my/resource";
        exposeUrl(resource_path);

        /* When */
        Reader readerFromURL = urlService.getReaderFromURL(HTTP_LOCALHOST + resource_path);

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

        /* When */  String lastUrl = urlService.getM3U8UrlFormMultiStreamFile(HTTP_LOCALHOST + "/my/ressources.m3u8");

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
        /* When */  String lastUrl = urlService.getM3U8UrlFormMultiStreamFile(HTTP_LOCALHOST + "/my/ressources2.m3u8");
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
        doRedirection("/my/ressources1.m3u8", HTTP_LOCALHOST + "/my/ressources2.m3u8");
        doRedirection("/my/ressources2.m3u8", HTTP_LOCALHOST + "/my/ressources3.m3u8");
        stubFor(get(urlEqualTo("/my/ressources3.m3u8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")));

        /* When */  String lastUrl = urlService.getRealURL(HTTP_LOCALHOST + "/my/ressources1.m3u8");
        /* Then */  assertThat(lastUrl).isEqualTo(HTTP_LOCALHOST + "/my/ressources3.m3u8");
    }

    @Test(expected = RuntimeException.class)
    public void should_recject_after_too_many_redirection() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", HTTP_LOCALHOST + "/my/ressources2.m3u8");
        doRedirection("/my/ressources2.m3u8", HTTP_LOCALHOST + "/my/ressources3.m3u8");
        doRedirection("/my/ressources3.m3u8", HTTP_LOCALHOST + "/my/ressources4.m3u8");
        doRedirection("/my/ressources4.m3u8", HTTP_LOCALHOST + "/my/ressources5.m3u8");
        doRedirection("/my/ressources5.m3u8", HTTP_LOCALHOST + "/my/ressources6.m3u8");
        doRedirection("/my/ressources6.m3u8", HTTP_LOCALHOST + "/my/ressources7.m3u8");
        doRedirection("/my/ressources7.m3u8", HTTP_LOCALHOST + "/my/ressources8.m3u8");
        doRedirection("/my/ressources8.m3u8", HTTP_LOCALHOST + "/my/ressources9.m3u8");
        doRedirection("/my/ressources9.m3u8", HTTP_LOCALHOST + "/my/ressources10.m3u8");
        doRedirection("/my/ressources10.m3u8", HTTP_LOCALHOST + "/my/ressources11.m3u8");

        /* When */ urlService.getRealURL(HTTP_LOCALHOST + "/my/ressources1.m3u8");
    }

    @Test
    public void should_treat_non_reachable_url() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", LOCALHOST_WITHOUT_PORT + "1234/my/ressources2.m3u8");

        /* When */  String lastUrl = urlService.getRealURL(HTTP_LOCALHOST + "/my/ressources1.m3u8");
        /* Then */  assertThat(lastUrl).isEqualTo(LOCALHOST_WITHOUT_PORT + "1234/my/ressources2.m3u8");
    }

    private void doRedirection(String mockUrl, String redirectionUrl) {
        stubFor(get(urlEqualTo(mockUrl))
                .willReturn(aResponse()
                        .withStatus(301)
                        .withHeader("Location", redirectionUrl)));
    }

    @Test
    public void should_get_connection_with_timeout() throws IOException {
        /* Given */
        String resource_path = "/my/resource";
        exposeUrl(resource_path);
        /* When */  URLConnection urlConnection = urlService.getConnectionWithTimeOut(HTTP_LOCALHOST + resource_path, 5000);
        /* Then */
        assertThat(urlConnection.getConnectTimeout()).isEqualTo(5000);
        assertThat(urlConnection.getReadTimeout()).isEqualTo(5000);
    }

    private void exposeUrl(String resource_path) {
        stubFor(get(urlEqualTo(resource_path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("12345678")));
    }

    @Test
    public void should_get_connection_with_default_timeout() throws IOException {
        /* Given */
        String resource_path = "/my/resource";
        exposeUrl(resource_path);

        /* When */
        URLConnection urlConnection = urlService.getConnection(HTTP_LOCALHOST + resource_path);

        /* Then */
        assertThat(urlConnection.getConnectTimeout()).isEqualTo(10000);
        assertThat(urlConnection.getReadTimeout()).isEqualTo(10000);
    }


    @Test
    public void should_get_filename_from_canal_url() {
        /* Given */ String url = "http://us-cplus-aka.canal-plus.com/i/1401/NIP_1960_,200k,400k,800k,1500k,.mp4.csmil/index_3_av.m3u8";
        /* When */  String fileNameFromCanalPlusM3U8Url = urlService.getFileNameM3U8Url(url);
        /* Then */  assertThat(fileNameFromCanalPlusM3U8Url).isEqualTo("NIP_1960_1500k.mp4");
    }

    @Test
    public void should_get_filename_from_m3u8_url() {
        /* Given */ String url = "http://ftvodhdsecz-f.akamaihd.net/i/streaming-adaptatif_france-dom-tom/2014/S42/J6/110996985-20141018-?null=";
        /* When */  String fileNameFromCanalPlusM3U8Url = urlService.getFileNameM3U8Url(url);
        /* Then */  assertThat(fileNameFromCanalPlusM3U8Url).isEqualTo("110996985-20141018-.mp4");
    }

    @Test
    public void should_return_url_with_specific_domain() {
        /* Given */ String domain = "http://www.google.fr/", subdomain = "subdomain";
        /* When */  String result = urlService.urlWithDomain(domain, subdomain);
        /* Then */  assertThat(result).isEqualTo("http://www.google.fr/subdomain");
    }

    @Test
    public void should_return_url_with_specific_domain_with_a_complete_url_as_subdomain() {
        /* Given */ String domain = "http://tty/", subdomain = "http://www.google.fr/subdomain";
        /* When */  String result = urlService.urlWithDomain(domain, subdomain);
        /* Then */  assertThat(result).isEqualTo("http://www.google.fr/subdomain");
    }

    @Test
    public void should_define_user_agent_on_construct() {
        /* When */ urlService.postConstruct();
        /* Then */ assertThat(System.getProperty("http.agent")).isEqualTo(HtmlService.USER_AGENT);
    }

}