package lan.dk.podcastserver.config;

import net.bramp.ffmpeg.FFmpegExecutor;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 13/06/2016 for Podcast Server
 */
public class FfmpegConfigTest {

    private FfmpegConfig ffmpegConfig = new FfmpegConfig();

    @Test
    public void should_have_a_bean_for_executor() throws IOException {
        /* Given */
        String ffmpeg = "/bin/bash"; // Replaced by bash for testing
        String ffprobe = "/bin/bash"; // Replaced by bash for testing

        /* When */
        FFmpegExecutor ffmpegExecutor = ffmpegConfig.ffmpeg(ffmpeg, ffprobe);

        /* Then */
        assertThat(ffmpegExecutor).isNotNull();
    }
}