package lan.dk.podcastserver.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.stream.Collectors.joining;
import static lan.dk.podcastserver.service.UrlService.USER_AGENT_DESKTOP;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 22/07/2016.
 */
public class UrlServiceTest {

    private static final Integer PORT = 8089;
    private static final String HTTP_LOCALHOST = "http://localhost:" + PORT;

    @Rule public WireMockRule wireMockRule = new WireMockRule(PORT);

    private UrlService urlService;

    @Before
    public void beforeEach() {
        urlService = new UrlService();
    }

    @Test
    public void should_execute_a_get_request() {
        /* Given */
        String url = "http://a.custom.url/foo/bar";

        /* When */
        GetRequest getRequest = urlService.get(url);

        /* Then */
        assertThat(getRequest).isNotNull();
        assertThat(getRequest.getUrl()).isEqualTo(url);
    }

    @Test
    public void should_execute_a_post_request() {
        /* Given */
        String url = "http://a.custom.url/foo/bar";

        /* When */
        HttpRequestWithBody postRequest = urlService.post(url);

        /* Then */
        assertThat(postRequest).isNotNull();
        assertThat(postRequest.getUrl()).isEqualTo(url);
    }

    @Test
    public void should_get_real_url_after_redirection() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", host("/my/ressources2.m3u8"));
        doRedirection("/my/ressources2.m3u8", host("/my/ressources3.m3u8"));
        stubFor(get(urlEqualTo("/my/ressources3.m3u8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")));

        /* When */
        String lastUrl = urlService.getRealURL(host("/my/ressources1.m3u8"));

        /* Then */
        assertThat(lastUrl).isEqualTo(host("/my/ressources3.m3u8"));
    }

    @Test
    public void should_get_real_url_after_redirection_with_user_agent() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", host("/my/ressources2.m3u8"));
        doRedirection("/my/ressources2.m3u8", host("/my/ressources3.m3u8"));
        stubFor(get(urlEqualTo("/my/ressources3.m3u8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")));

        /* When */
        String lastUrl = urlService.getRealURL(host("/my/ressources1.m3u8"), c -> c.setRequestProperty("User-Agent", USER_AGENT_DESKTOP));
        /* Then */
        assertThat(lastUrl).isEqualTo(host("/my/ressources3.m3u8"));
    }
    
    @Test(expected = RuntimeException.class)
    public void should_recject_after_too_many_redirection() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", host("/my/ressources2.m3u8"));
        doRedirection("/my/ressources2.m3u8", host("/my/ressources3.m3u8"));
        doRedirection("/my/ressources3.m3u8", host("/my/ressources4.m3u8"));
        doRedirection("/my/ressources4.m3u8", host("/my/ressources5.m3u8"));
        doRedirection("/my/ressources5.m3u8", host("/my/ressources6.m3u8"));
        doRedirection("/my/ressources6.m3u8", host("/my/ressources7.m3u8"));
        doRedirection("/my/ressources7.m3u8", host("/my/ressources8.m3u8"));
        doRedirection("/my/ressources8.m3u8", host("/my/ressources9.m3u8"));
        doRedirection("/my/ressources9.m3u8", host("/my/ressources10.m3u8"));
        doRedirection("/my/ressources10.m3u8", host("/my/ressources11.m3u8"));

        /* When */ urlService.getRealURL(host("/my/ressources1.m3u8"));
    }
    
    @Test
    public void should_get_url_as_reader() throws IOException {
        /* Given */
        stubFor(get(urlEqualTo("/file.txt"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")
                        .withBody("A body for testing")
                )
        );

        /* When */
        BufferedReader br = urlService.asReader(HTTP_LOCALHOST + "/file.txt");

        /* Then */
        assertThat(br.lines().collect(joining())).isEqualTo("A body for testing");
    }

    private void doRedirection(String mockUrl, String redirectionUrl) {
        stubFor(
                get(urlEqualTo(mockUrl))
                        .willReturn(aResponse()
                        .withStatus(301)
                        .withHeader("Location", redirectionUrl))
        );
    }

    private static String host(String path) {
        return HTTP_LOCALHOST + path;
    }
}