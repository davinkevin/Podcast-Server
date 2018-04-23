package lan.dk.podcastserver.service.properties;

import org.junit.Before;
import org.junit.Test;

import static lan.dk.podcastserver.service.properties.ApiAssert.assertThat;

/**
 * Created by kevin on 13/04/2016 for Podcast Server
 */
public class ApiTest {

    Api api;

    @Before
    public void beforeEach() {
        api = new Api();
    }

    @Test
    public void should_have_default_value() {
        /* Given */
        /* When */
        /* Then */
        assertThat(api)
                .hasYoutube(null)
                .hasDailymotion(null);
    }

    @Test
    public void should_have_specified_values() {
        /* Given */
        String youtubeKey = "YoutubeKey";
        String dailymotionKey = "dailymotionKey";

        /* When */
        api.setDailymotion(dailymotionKey).setYoutube(youtubeKey);

        /* Then */
        assertThat(api)
                .hasYoutube(youtubeKey)
                .hasDailymotion(dailymotionKey);
    }

}
