package lan.dk.podcastserver.service;

import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 31/03/2016 for Podcast Server
 */
public class ProcessServiceTest {

    private ProcessService processService;

    @Before
    public void beforeEach() {
        processService = new ProcessService();
    }

    @Test
    public void should_create_process_with_varargs() {
        /* Given */
        /* When */
        ProcessBuilder processBuilder = processService.newProcessBuilder("foo", "bar");

        /* Then */
        assertThat(processBuilder.command()).contains("foo", "bar");
    }

    @Test
    public void should_get_pid() throws IOException {
        /* Given */
        Process ls = processService.newProcessBuilder("ls", "-al").start();

        /* When */
        int pid = processService.pidOf(ls);

        /* Then */
        assertThat(pid).isGreaterThan(0);
    }

    @Test
    public void should_return_invalid_value_if_not_on_unix() {
        /* Given */
        Process process = mock(Process.class);

        /* When */
        int pid = processService.pidOf(process);

        /* Then */
        assertThat(pid).isEqualTo(-1);
    }

    @Test
    public void should_start_a_process() {
        /* Given */
        ProcessBuilder processBuilder = new ProcessBuilder("echo", "foo");

        /* When */
        Try<Process> tryProcess = processService.start(processBuilder);

        /* Then */
        assertThat(tryProcess.isSuccess()).isTrue();
    }

    @Test
    public void should_wait_for_process() throws InterruptedException {
        /* Given */
        Process process = mock(Process.class);
        when(process.waitFor()).thenReturn(10);

        /* When */
        Try<Integer> tryProcess = processService.waitFor(process);

        /* Then */
        assertThat(tryProcess.get()).isEqualTo(10);
        verify(process).waitFor();
    }
}
