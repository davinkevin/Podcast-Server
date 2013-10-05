package lan.dk.podcastserver.worker.externalTools;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;


public class RtmpDumpTest {

    private final Logger logger = LoggerFactory.getLogger(RtmpDumpTest.class);
    public static final String CHEMIN = "/Users/kdavin/";

        @Test
        public void main() {
            try {
                ProcessBuilder pb = new ProcessBuilder("/usr/local/bin/rtmpdump", "-r", "rtmp://vod-fms.canalplus.fr/ondemand/videos/1309/LE_GRAND_JOURNAL_LA_METEO_DE_DORIA_130920_CAN_365325_video_HD.mp4", "-o", "test"+ System.currentTimeMillis()+".mp4");
                pb.directory(new File(CHEMIN));

                Map env = pb.environment();
//                for (Entry entry : env.entrySet()) {
//                    System.out.println(entry.getKey() + " : " + entry.getValue());
//                }
//                env.put("MonArg", "Valeur");

                Process p = pb.start();
                AfficheurFlux fluxSortie = new AfficheurFlux(p.getInputStream());
                AfficheurFlux fluxErreur = new AfficheurFlux(p.getErrorStream());
                new Thread(fluxSortie).start();
                new Thread(fluxErreur).start();
                p.waitFor();

                //Thread.sleep(1000L);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    class AfficheurFlux implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(AfficheurFlux.class);
        private final InputStream inputStream;

        AfficheurFlux(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        private BufferedReader getBufferedReader(InputStream is) {
            return new BufferedReader(new InputStreamReader(is));
        }

        @Override
        public void run() {
            BufferedReader br = getBufferedReader(inputStream);
            String ligne = "";
            //logger.debug("Lecture du stream");
            try {
                while ((ligne = br.readLine()) != null) {
                    logger.debug(ligne);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


