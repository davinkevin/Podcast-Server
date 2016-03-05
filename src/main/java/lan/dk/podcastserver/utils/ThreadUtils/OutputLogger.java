package lan.dk.podcastserver.utils.ThreadUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * Created by kevin on 19/07/2014.
 */
@Slf4j
public class OutputLogger implements Runnable {
    private final InputStream inputStream;

    public OutputLogger(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    private BufferedReader getBufferedReader(InputStream is) {
        return new BufferedReader(new InputStreamReader(is));
    }

    @Override
    public void run() {
        try(BufferedReader br = getBufferedReader(inputStream)) {
            br.lines().forEach(log::info);
        } catch (IOException | UncheckedIOException e) {
            e.printStackTrace();
        }
    }

}
