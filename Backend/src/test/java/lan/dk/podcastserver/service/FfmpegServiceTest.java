package lan.dk.podcastserver.service;

import lan.dk.podcastserver.utils.custom.ffmpeg.CustomRunProcessFunc;
import lan.dk.podcastserver.utils.custom.ffmpeg.ProcessListener;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 20/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class FfmpegServiceTest {

    @Mock CustomRunProcessFunc runFunc;
    @Mock FFprobe ffprobe;
    @Mock FFmpegExecutor ffmpegExecutor;
    @InjectMocks FfmpegService ffmpegService;

    @Captor ArgumentCaptor<FFmpegBuilder> executorBuilderCaptor;

    @Test
    public void should_concat_files() {
        /* Given */
        FFmpegJob job = mock(FFmpegJob.class);
        when(ffmpegExecutor.createJob(any())).thenReturn(job);

        Path output = Paths.get("/tmp/output.mp4");
        Path input1 = Paths.get("/tmp/input1.mp4");
        Path input2 = Paths.get("/tmp/input2.mp4");
        Path input3 = Paths.get("/tmp/input3.mp4");


        /* When */
        ffmpegService.concat(output, input1, input2, input3);

        /* Then */
        verify(ffmpegExecutor, times(1)).createJob(executorBuilderCaptor.capture());
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
        ffmpegService.concat(output, input1);

        /* Then */
        // Nothing done
    }

    @Test
    public void should_merge_audio_and_video() {
        /* Given */
        Path video = Paths.get("/tmp/bar.mp4");
        Path audio = Paths.get("/tmp/bar.webm");
        Path dest = Paths.get("/tmp/foo.mp4");

        FFmpegJob job = mock(FFmpegJob.class);
        when(ffmpegExecutor.createJob(any())).thenReturn(job);

        /* When */
        Path generatedFile = ffmpegService.mergeAudioAndVideo(video, audio, dest);

        /* Then */
        assertThat(generatedFile).isEqualTo(dest);
    }

    @Test(expected = RuntimeException.class)
    public void should_not_merge_if_folder_is_read_only() {
          /* Given */
        Path video = Paths.get("/tmp/bar.mp4");
        Path audio = Paths.get("/tmp/bar.webm");
        Path dest = Paths.get("/foo.mp4");

        FFmpegJob job = mock(FFmpegJob.class);
        when(ffmpegExecutor.createJob(any())).thenReturn(job);

        /* When */
        ffmpegService.mergeAudioAndVideo(video, audio, dest);

        /* Then */
        /* @See Exception in annotation */
    }

    @Test
    public void should_get_duration() throws IOException {
        /* Given */
        FFmpegProbeResult result = new FFmpegProbeResult();
        result.format = new FFmpegFormat();
        result.format.duration = 987;
        when(ffprobe.probe(anyString(), anyString())).thenReturn(result);

        /* When */
        double duration = ffmpegService.getDurationOf("foo", "bar");

        /* Then */
        assertThat(duration).isEqualTo(987*1_000_000);
        verify(ffprobe, only()).probe(eq("foo"), eq("bar"));
    }

    @Test(expected = UncheckedIOException.class)
    public void should_throw_exception_if_ffprobe_problem() throws IOException {
        /* Given */
        doThrow(IOException.class).when(ffprobe).probe(anyString(), anyString());

        /* When */
        ffmpegService.getDurationOf("foo", "bar");

        /* Then see @Test */
    }

    @Test
    public void should_download_with_ffmpeg() {
        /* Given */
        FFmpegJob job = mock(FFmpegJob.class);
        when(runFunc.add(any(ProcessListener.class))).then(i -> {
            ProcessListener pl = i.getArgumentAt(0, ProcessListener.class);
            pl.process(mock(Process.class));
            return runFunc;
        });
        when(ffmpegExecutor.createJob(any(), any())).then(i -> job);

        /* When */
        Process p = ffmpegService.download("foo", new FFmpegBuilder(), d -> {});

        /* Then */
        assertThat(p).isNotNull();
    }

}
