package lan.dk.podcastserver.config;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Created by kevin on 21/05/2016 for Podcast Server
 */
@Configuration
public class FfmpegConfig {

    @Bean
    public FFmpegExecutor ffmpeg(
            @Value("${podcastserver.externaltools.ffmpeg:/usr/local/bin/ffmpeg}") String ffmpegLocation,
            @Value("${podcastserver.externaltools.ffprobe:/usr/local/bin/ffprobe}") String ffprobeLocation ) throws IOException {
        FFmpeg ffmpeg = new FFmpeg(ffmpegLocation);
        FFprobe ffprobe = new FFprobe(ffprobeLocation);
        return new FFmpegExecutor(ffmpeg, ffprobe);
    }

}
