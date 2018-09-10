package lan.dk.podcastserver.config;

import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.CustomRunProcessFunc;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by kevin on 13/06/2016 for Podcast Server
 */
public class FfmpegConfigTest {

    private FfmpegConfig ffmpegConfig = new FfmpegConfig();

    @Test
    public void should_have_a_bean_for_executor() {
        /* Given */
        FFmpeg ffmpeg = mock(FFmpeg.class);
        FFprobe ffprobe = mock(FFprobe.class);

        /* When */
        FFmpegExecutor ffmpegExecutor = ffmpegConfig.ffmpegExecutor(ffmpeg, ffprobe);

        /* Then */
        assertThat(ffmpegExecutor).isNotNull();
    }

    @Test
    public void should_generate_ffmpeg() throws IOException {
        /* Given */
        String binary = "/bin/bash";

        /* When */
        FFmpeg ffmpeg = ffmpegConfig.ffmpeg(binary, new CustomRunProcessFunc());

        /* Then */
        assertThat(ffmpeg).isNotNull();
    }

    @Test
    public void should_generate_ffprobe() {
        /* Given */
        String binary = "/bin/bash";

        /* When */
        FFprobe ffprobe = ffmpegConfig.ffprobe(binary, new CustomRunProcessFunc());

        /* Then */
        assertThat(ffprobe).isNotNull();
    }

    @Test
    public void should_have_a_run_process_func() {
        assertThat(ffmpegConfig.runProcessFunc()).isNotNull();
    }
}
