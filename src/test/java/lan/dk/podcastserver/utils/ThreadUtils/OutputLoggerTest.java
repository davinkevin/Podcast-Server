package lan.dk.podcastserver.utils.ThreadUtils;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 16/07/15 for Podcast Server
 */
public class OutputLoggerTest {

    @Test
    public void should_log_data_from_inputStream_withou_log() {
        /* Given */
        InputStream in = IOUtils.toInputStream("some test data for my input stream");
        OutputLogger outputLogger = new OutputLogger(in);

        /* When */  outputLogger.run();
    }

    @Test
    public void should_exit_if_exception() throws IOException {
        /* Given */
        InputStream in = mock(InputStream.class);
        when(in.read()).thenThrow(new IOException());
        OutputLogger outputLogger = new OutputLogger(in);

        /* When */  outputLogger.run();
    }

}