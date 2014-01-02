package lan.dk.podcastserver.worker.externalTools;

import com.sun.tools.doclets.internal.toolkit.util.DocFinder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by kevin on 31/12/2013.
 */
public class TestM3u8 {
    private final Logger logger = LoggerFactory.getLogger(TestM3u8.class);
    public static final String CHEMIN = "/Users/kdavin/";

    @Test
    public void concatStream() throws IOException {

        logger.debug("Création des listes de streams");
        List<InputStream> listFlux = new ArrayList<InputStream>();


        OutputStream outputStream = new FileOutputStream(new File("/Users/kevin/testDownload.mp4"));

      /*  URL oracle = new URL("http://us-cplus-aka.canal-plus.com/i/1312/LE_PETIT_JOURNAL_QUOTIDIEN_131224_CAN_393258_video_,MOB,L,H,HD,.mp4.csmil/index_3_av.m3u8");
        BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (!inputLine.contains("#EXT")) {
                logger.debug("Ajout de l'url : " + inputLine);
                listFlux.add(new URL(inputLine).openStream());
            }
        }
        in.close();

        SequenceInputStream se = new SequenceInputStream(Collections.enumeration(listFlux));
        int read = 0;
        byte[] bytes = new byte[1024];
        logger.debug("Lecture des stream vers fichiers");
        while ((read = se.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }*/

        URL oracle = new URL("http://us-cplus-aka.canal-plus.com/i/1312/LE_PETIT_JOURNAL_QUOTIDIEN_131224_CAN_393258_video_,MOB,L,H,HD,.mp4.csmil/index_3_av.m3u8");
        BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (!inputLine.contains("#EXT")) {
                logger.debug("Ajout de l'url : " + inputLine);
                InputStream is = new URL(inputLine).openStream();
                int read = 0;
                byte[] bytes = new byte[1024];
                logger.debug("Lecture des stream vers fichiers");
                while ((read = is.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
                is.close();
            }
        }
        in.close();
        outputStream.close();

    }
}
