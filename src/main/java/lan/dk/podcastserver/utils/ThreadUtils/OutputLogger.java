package lan.dk.podcastserver.utils.ThreadUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by kevin on 19/07/2014.
 */
public class OutputLogger implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final InputStream inputStream;

    public OutputLogger(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    private BufferedReader getBufferedReader(InputStream is) {
        return new BufferedReader(new InputStreamReader(is));
    }

    @Override
    public void run() {
        BufferedReader br = getBufferedReader(inputStream);
        String ligne = "";
        try {
            while ((ligne = br.readLine()) != null) {
                logger.info(ligne);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
