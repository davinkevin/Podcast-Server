package lan.dk.podcastserver.utils.custom.ffmpeg;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static io.vavr.API.List;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 24/07/2016.
 */
public class CustomRunProcessFuncTest {

    @Test
    public void should_get_process_from_listener() throws IOException, ExecutionException, InterruptedException {
        /* Given */
        CustomRunProcessFunc cp = new CustomRunProcessFunc();
        ProcessListener pl = new ProcessListener("anUrl");

        /* When */
        Process p = cp.add(pl).run(List("/bin/bash", "anUrl", "Foo", "Bar").toJavaList());

        /* Then */
        assertThat(p).isSameAs(pl.process());
        assertThat(pl.getProcess().get()).isSameAs(p);
    }

}
