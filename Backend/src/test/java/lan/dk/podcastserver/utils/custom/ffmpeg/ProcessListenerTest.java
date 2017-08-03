package lan.dk.podcastserver.utils.custom.ffmpeg;

import io.vavr.control.Try;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by kevin on 24/07/2016.
 */
public class ProcessListenerTest {

    @Test
    public void should_wait_if_no_process() {
        /* Given */
        ProcessListener pl = new ProcessListener("foo");
        Process aProcess = mock(Process.class);
        final Process[] process = {null};

        /* When */
        runAsync(() -> {
            try { process[0] = pl.getProcess().get(); } catch (InterruptedException | ExecutionException ignored) {}
        });
        Try.run(() -> TimeUnit.MILLISECONDS.sleep(200));
        pl.process(aProcess);

        /* Then */
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            assertThat(process[0]).isNotNull();
        });
    }

}
