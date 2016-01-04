package lan.dk.podcastserver.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 11/06/15 for HackerRank problem
 */
public class HtmlServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089); // No-args constructor defaults to port 8080

    public static final String URL = "http://nowhere.anywhere";
    public static final String USER_AGENT = "User-Agent";
    public static final String REFERER = "Referer";
    HtmlService htmlService;

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
    public void should_not_have_default_parameters () {
        /* When */ HttpConnection connection = (HttpConnection) htmlService.connect(URL);
        /* Then */
            assertThat(connection.request().header(USER_AGENT)).isNull();
            assertThat(connection.request().header(REFERER)).isNull();
            assertThat(connection.request().timeout()).isNotEqualTo(5000);
    }

    @Test
    public void should_parse_document() throws IOException {
        /* When */
        Document document = htmlService.get("http://localhost:8089/service/htmlService/jsoup.html");

        /* Then */
        assertThat(document.select("title").first().text()).isEqualTo("JSOUP Example");
    }

}