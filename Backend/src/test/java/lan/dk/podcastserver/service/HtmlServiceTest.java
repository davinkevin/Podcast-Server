package lan.dk.podcastserver.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 11/06/15 for HackerRank problem
 */
public class HtmlServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089); // No-args constructor defaults to port 8080

    public static final String URL = "http://nowhere.anywhere";
    private static final String USER_AGENT = "User-Agent";
    private static final String REFERER = "Referer";
    private HtmlService htmlService;

    @Before
    public void beforeEach() {
        /* Given */
        htmlService = new HtmlService();
    }

    @Test
    public void should_generate_default_connection() {
        /* When */ HttpConnection connection = (HttpConnection) htmlService.connectWithDefault(URL);

        /* Then */
            assertThat(connection.request().header(USER_AGENT)).isEqualTo(HtmlService.USER_AGENT);
            assertThat(connection.request().header(REFERER)).isEqualTo("http://www.google.fr");
            assertThat(connection.request().timeout()).isEqualTo(5000);
    }

    @Test
    public void should_get_page() {
        /* Given */
        stubFor(get(urlEqualTo("/page/file.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBodyFile("service/htmlService/jsoup.html")));

        /* When */
        Optional<Document> document = htmlService.get("http://localhost:8089/page/file.html");

        /* Then */
        assertThat(document).isPresent();
        assertThat(document.map(d -> d.head().select("title").text())).hasValue("JSOUP Example");
    }

    @Test
    public void should_reject_get_with_empty() {
        /* Given */
        /* When */
        Optional<Document> document = htmlService.get("http://localhost:8089/page/foo.html");

        /* Then */
        assertThat(document).isEmpty();
    }

    @Test
    public void should_parse_string() {
        /* Given */
        String html = "<div></div>";
        /* When */
        Document document = htmlService.parse(html);
        /* Then */
        assertThat(document).isNotNull().isInstanceOf(Document.class);
    }

}