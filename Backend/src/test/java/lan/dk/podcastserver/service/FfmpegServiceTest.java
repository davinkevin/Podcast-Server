package lan.dk.podcastserver.service;

import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 20/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class FfmpegServiceTest {

    @Mock FFmpegExecutor fFmpegExecutor;
    @InjectMocks FfmpegService ffmpegService;

    @Captor ArgumentCaptor<FFmpegBuilder> executorBuilderCaptor;

    @Test
    public void should_concat_files() {
        /* Given */
        FFmpegJob job = mock(FFmpegJob.class);
        when(fFmpegExecutor.createJob(any())).thenReturn(job);

        Path output = Paths.get("/tmp/output.mp4");
        Path input1 = Paths.get("/tmp/input1.mp4");
        Path input2 = Paths.get("/tmp/input2.mp4");
        Path input3 = Paths.get("/tmp/input3.mp4");


        /* When */
        ffmpegService.concatDemux(output, input1, input2, input3);

        /* Then */
        verify(fFmpegExecutor, times(1)).createJob(executorBuilderCaptor.capture());
        verify(job, times(1)).run();
        assertThat(executorBuilderCaptor.getValue().build()).contains(
                "-f", "concat",
                "-i",
                "-vcodec", "copy",
                "-acodec", "copy",
                "/tmp/output.mp4"
        );
        assertThat(
            executorBuilderCaptor.getValue().build().stream().anyMatch(s -> s.startsWith("/tmp/ffmpeg-list-") && s.endsWith(".txt"))
        ).isTrue();
    }

    @Test
    public void should_catch_error_if_problem() {
        /* Given */
        Path output = Paths.get("/bin/bash");
        Path input1 = Paths.get("/tmp/input1.mp4");

        /* When */
        ffmpegService.concatDemux(output, input1);

        /* Then */
        // Nothing done
    }

}