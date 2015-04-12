package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("RTMPDownloader")
@Scope("prototype")
public class RTMPDownloader extends AbstractDownloader {

    private int pid = 0;

    @Value("${podcastserver.externaltools.rtmpdump:/usr/local/bin/rtmpdump}")
    String rtmpdump;

    Process p = null;

    @Override
    public Item download() {
        logger.debug("Download");
        itemDownloadManager.addACurrentDownload();

        if (item.getUrl().contains("rtmp://")) {
            try {
                target = getTagetFile(item);
                ProcessBuilder pb = new ProcessBuilder(rtmpdump,
                        "-r",
                        item.getUrl(),
                        "-o",
                        target.getAbsolutePath());

                pb.directory(new File("/tmp"));
                logger.debug("Fichier de sortie : " + target.getAbsolutePath());

                p  = pb.start();
                if (p.getClass().getSimpleName().contains("UNIXProcess")) {
                    Field pidField = p.getClass().getDeclaredField("pid");
                    pidField.setAccessible(true);
                    pid = pidField.getInt(p);
                    logger.debug("PID du process : " + pid);
                }

                Runnable itemErrorSynchronisation = new Runnable() {
                    private BufferedReader getBufferedReader(InputStream is) {
                        return new BufferedReader(new InputStreamReader(is));
                    }

                    @Override
                    public void run() {
                        BufferedReader br = getBufferedReader(p.getErrorStream());
                        String ligne = "";
                        Pattern p = Pattern.compile("[^\\(]*\\(([0-9]*).*%\\)");
                        Matcher m = null;
                        logger.debug("Lecture du stream d'erreur");
                        try {
                            long time = System.currentTimeMillis();
                            while ((ligne = br.readLine()) != null) {
                                //logger.debug(ligne);
                                m = p.matcher(ligne);
                                // Si le dernier traitement date de plus d'une seconde :
                                if (m.matches() && Integer.parseInt(m.group(1)) > item.getProgression()) {
                                    item.setProgression(Integer.parseInt(m.group(1)));
                                    logger.debug("Item Progression : " + item.getProgression());
                                    convertAndSaveBroadcast();
                                } else if (ligne.toLowerCase().contains("download complete")) {
                                    logger.info("Fin du téléchargement");
                                    finishDownload();
                                    break;
                                }
                                if (pid == 0) {
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            logger.error("IOException :", e);
                        }
                        if (!Status.FINISH.is(item.getStatus()) && !stopDownloading.get()) {
                            logger.debug("Terminaison innatendu, reset du downloader");
                            resetDownload();
                        }
                    }
                };
                itemErrorSynchronisation.run();
                p.waitFor();
                pid = 0;

            } catch (IOException e) {
                e.printStackTrace();
                logger.error("IOException :", e);
                stopDownload();
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.error("InterruptedException :", e);
                stopDownload();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                logger.error("NoSuchFieldException :", e);
                stopDownload();
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                logger.error("IllegalAccessException :", e);
                stopDownload();
            }
        } else {
            logger.debug("Traiter avec un téléchargeur HTTP");
            stopDownload();
        }
        return this.item;
    }
    @Override
    public void startDownload() {
        stopDownloading.set(false);
        this.item.setStatus(Status.STARTED);
        this.saveSyncWithPodcast();
        if (pid != 0 && p != null) { //Relancement du process UNIX
            //ProcessBuilder pb = new ProcessBuilder("kill", "-CONT", "" + pid);
            logger.debug("Reprise du téléchargement");
            p.destroy();
        } else { // Lancement du process simple
            this.download();
        }
    }

    @Override
    public void pauseDownload() {
        ProcessBuilder pb = new ProcessBuilder("kill", "-STOP", "" + pid);
        try {
            pb.start();
            super.pauseDownload();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
            this.stopDownload();
        }
    }

    @Override
    public void stopDownload() {
        try {
            p.destroy();
        } finally {
            super.stopDownload();
        }

    }

}
