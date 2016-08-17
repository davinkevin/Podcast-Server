package lan.dk.podcastserver.service.factory;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by kevin on 31/03/2016 for Podcast Server
 */
public class ProcessBuilderFactoryTest {

    private ProcessBuilderFactory processBuilderFactory;

    @Before
    public void beforeEach() {
        processBuilderFactory = new ProcessBuilderFactory();
    }

    @Test
    public void should_create_process_with_varargs() {
        /* Given */
        /* When */
        ProcessBuilder processBuilder = processBuilderFactory.newProcessBuilder("foo", "bar");

        /* Then */
        assertThat(processBuilder.command()).contains("foo", "bar");
    }

    @Test
    public void should_get_pid() throws IOException {
        /* Given */
        Process ls = processBuilderFactory.newProcessBuilder("ls", "-al").start();

        /* When */
        int pid = processBuilderFactory.pidOf(ls);

        /* Then */
        assertThat(pid).isGreaterThan(0);
    }

    @Test
    public void should_return_invalid_value_if_not_on_unix() {
        /* Given */
        Process process = mock(Process.class);

        /* When */
        int pid = processBuilderFactory.pidOf(process);

        /* Then */
        assertThat(pid).isEqualTo(-1);
    }
}