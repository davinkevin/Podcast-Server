package lan.dk.podcastserver.config;

import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.CustomRunProcessFunc;
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
    public FFmpegExecutor ffmpegExecutor(FFmpeg ffmpeg, FFprobe ffprobe) {
        return new FFmpegExecutor(ffmpeg, ffprobe);
    }

    @Bean
    public FFmpeg ffmpeg(@Value("${podcastserver.externaltools.ffmpeg:/usr/local/bin/ffmpeg}") String ffmpegLocation, CustomRunProcessFunc runProcessFunc) throws IOException {
        return new FFmpeg(ffmpegLocation, runProcessFunc);
    }

    @Bean
    public FFprobe ffprobe(@Value("${podcastserver.externaltools.ffprobe:/usr/local/bin/ffprobe}") String ffprobeLocation, CustomRunProcessFunc runProcessFunc) {
        return new FFprobe(ffprobeLocation, runProcessFunc);
    }

    @Bean
    public CustomRunProcessFunc runProcessFunc() {
        return new CustomRunProcessFunc();
    }

}
