package lan.dk.podcastserver.service;

import org.jsoup.helper.HttpConnection;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 11/06/15 for HackerRank problem
 */
public class HtmlServiceTest {

    public static final String URL = "http://nowhere.anywhere";
    public static final String USER_AGENT = "User-Agent";
    public static final String REFERER = "Referer";

    @Test
    public void should_generate_default_connection() {
        /* Given */ HtmlService htmlService = new HtmlService();
        /* When */ HttpConnection connection = (HttpConnection) htmlService.connectWithDefault(URL);

        /* Then */
            assertThat(connection.request().header(USER_AGENT)).isEqualTo(HtmlService.USER_AGENT);
            assertThat(connection.request().header(REFERER)).isEqualTo("http://www.google.fr");
            assertThat(connection.request().timeout()).isEqualTo(5000);
    }

    @Test
    public void should_not_have_default_parameters () {
        /* Given */ HtmlService htmlService = new HtmlService();
        /* When */ HttpConnection connection = (HttpConnection) htmlService.connect(URL);
        /* Then */
            assertThat(connection.request().header(USER_AGENT)).isNull();
            assertThat(connection.request().header(REFERER)).isNull();
            assertThat(connection.request().timeout()).isNotEqualTo(5000);
    }

}