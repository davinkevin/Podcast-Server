package lan.dk.podcastserver.service.properties;

import org.junit.Before;
import org.junit.Test;

import static lan.dk.podcastserver.service.properties.ExternalToolsAssert.assertThat;

/**
 * Created by kevin on 13/04/2016 for Podcast Server
 */
public class ExternalToolsTest {

    ExternalTools externalTools;

    @Before
    public void beforeEach() {
        externalTools = new ExternalTools();
    }

    @Test
    public void should_have_default_value() {
        assertThat(externalTools)
                .hasFfmpeg("/usr/local/bin/ffmpeg")
                .hasRtmpdump("/usr/local/bin/rtmpdump");
    }

    @Test
    public void should_change_value() {
        /* Given */
        /* When */
        externalTools.setFfmpeg("/tmp/ffmpeg").setRtmpdump("/tmp/rtmpdump");

        /* Then */
        assertThat(externalTools)
                .hasFfmpeg("/tmp/ffmpeg")
                .hasRtmpdump("/tmp/rtmpdump");
    }
}
