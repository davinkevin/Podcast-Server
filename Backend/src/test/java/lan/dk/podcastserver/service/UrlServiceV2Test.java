package lan.dk.podcastserver.service;

import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 22/07/2016.
 */
public class UrlServiceV2Test {

    UrlServiceV2 urlServiceV2;

    @Before
    public void beforeEach() {
        urlServiceV2 = new UrlServiceV2();
    }

    @Test
    public void should_execute_a_get_request() {
        /* Given */
        String url = "http://a.custom.url/foo/bar";

        /* When */
        GetRequest getRequest = urlServiceV2.get(url);

        /* Then */
        assertThat(getRequest).isNotNull();
        assertThat(getRequest.getUrl()).isEqualTo(url);
    }

    @Test
    public void should_execute_a_post_request() {
        /* Given */
        String url = "http://a.custom.url/foo/bar";

        /* When */
        HttpRequestWithBody postRequest = urlServiceV2.post(url);

        /* Then */
        assertThat(postRequest).isNotNull();
        assertThat(postRequest.getUrl()).isEqualTo(url);
    }

}