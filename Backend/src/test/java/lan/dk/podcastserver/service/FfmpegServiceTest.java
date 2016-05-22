package lan.dk.podcastserver.service;

import lan.dk.podcastserver.service.factory.ProcessBuilderFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 20/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class FfmpegServiceTest {

    @Mock ProcessBuilderFactory processBuilderFactory;
    @InjectMocks FfmpegService ffmpegService;

    @Captor ArgumentCaptor<String> processParameters;

    @Before
    public void beforeEach() {
        ffmpegService.ffmpeg = "/usr/local/bin/ffmpeg";
    }

    @Test
    public void should_concat_files() {
        /* Given */
        File output = new File("/tmp/output.mp4");
        File input1 = new File("/tmp/input1.mp4");
        File input2 = new File("/tmp/input2.mp4");
        File input3 = new File("/tmp/input3.mp4");

        when(processBuilderFactory.newProcessBuilder((String[]) anyVararg())).thenReturn(new ProcessBuilder("ls"));

        /* When */
        ffmpegService.concatDemux(output, input1, input2, input3);

        /* Then */
        verify(processBuilderFactory, times(1)).newProcessBuilder(processParameters.capture());
        assertThat(processParameters.getAllValues()).contains(
                ffmpegService.ffmpeg,
                "-f", "concat",
                "-i",
                "-vcodec", "copy",
                "-acodec", "copy",
                "/tmp/output.mp4"
        );
        assertThat(
            processParameters.getAllValues().stream().anyMatch(s -> s.startsWith("/tmp/ffmpeg-list") && s.endsWith(".txt"))
        ).isTrue();
    }

    @Test
    public void should_catch_error_if_problem() {
        /* Given */
        File output = new File("/bin/bash");
        File input1 = new File("/tmp/input1.mp4");

        /* When */
        ffmpegService.concatDemux(output, input1);

        /* Then */
        // Nothing done
    }

}