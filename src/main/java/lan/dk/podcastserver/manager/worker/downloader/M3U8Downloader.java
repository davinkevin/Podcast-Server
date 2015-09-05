package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.utils.URLUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Scope("prototype")
@Component("M3U8Downloader")
public class M3U8Downloader extends AbstractDownloader {

    List<String> urlList;
    OutputStream outputStream;
    Runnable runnableDownloader;


    @Override
    public Item download() {
        logger.debug("Download");
        itemDownloadManager.addACurrentDownload();

        if (getItemUrl().contains("m3u8")) {
            URL urlListFile = null;
            try {
                urlListFile = new URL(getItemUrl());
                urlList = new ArrayList<>();

                BufferedReader in = new BufferedReader(new InputStreamReader(urlListFile.openStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (!inputLine.startsWith("#")) {
                        urlList.add(URLUtils.urlWithDomain(getItemUrl(), inputLine));
                    }
                }
                in.close();

                // Tous les éléments sont disponibles dans urlList :
                target = getTagetFile(item);

                outputStream = new FileOutputStream(target);
                runnableDownloader = new Runnable() {
                    @Override
                    public void run() {

                        try {
                            for (int cpt = 0; cpt < urlList.size(); cpt++) {
                                String urlFragmentToDownload = urlList.get(cpt);

                                logger.debug("URL : {}", urlFragmentToDownload);
                                try {
                                    InputStream is = new URL(urlFragmentToDownload).openStream();
                                    int read = 0;
                                    byte[] bytes = new byte[1024];

                                    while ((read = is.read(bytes)) != -1) {
                                        outputStream.write(bytes, 0, read);
                                    }
                                    item.setProgression((100*cpt)/urlList.size());
                                    logger.debug("Progression : {}", item.getProgression());
                                    convertAndSaveBroadcast();
                                    is.close();
                                } catch (FileNotFoundException e) {
                                    logger.error("Fichier introuvable : {}", urlFragmentToDownload, e);
                                }


                                if (stopDownloading.get()) {
                                    if (Status.STOPPED == item.getStatus()) {
                                        break;
                                    } else if (Status.PAUSED == item.getStatus()) {

                                        synchronized(this)
                                        {
                                            this.wait();
                                        }
                                    }
                                }
                            }

                            if (Status.STARTED == item.getStatus()) {
                                finishDownload();
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };

                runnableDownloader.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            logger.debug("Traiter avec le downloader adapté à {}", getItemUrl());
            stopDownload();
        }
        return this.item;
    }

    @Override
    public File getTagetFile (Item item) {

        if (target != null)
            return target;

        String fileNameFromM3U8Playlist = URLUtils.getFileNameM3U8Url(item.getUrl());

        File finalFile = new File(itemDownloadManager.getRootfolder() + File.separator + item.getPodcast().getTitle() + File.separator + fileNameFromM3U8Playlist);
        logger.debug("Création du fichier : {}", finalFile.getAbsolutePath());
        //logger.debug(file.getAbsolutePath());

        if (!finalFile.getParentFile().exists()) {
            finalFile.getParentFile().mkdirs();
        }

        if (finalFile.exists() || new File(finalFile.getAbsolutePath().concat(temporaryExtension)).exists()) {
            logger.info("Doublon sur le fichier en lien avec {} - {}, {}", item.getPodcast().getTitle(), item.getId(), item.getTitle() );
            try {
                finalFile  = File.createTempFile(
                        FilenameUtils.getBaseName(fileNameFromM3U8Playlist).concat("-"),
                        ".".concat(FilenameUtils.getExtension(fileNameFromM3U8Playlist)),
                        finalFile.getParentFile());
                finalFile.delete();
            } catch (IOException e) {
                logger.error("Erreur lors du renommage d'un doublon", e);
            }
        }

        return new File(finalFile.getAbsolutePath() + temporaryExtension) ;

    }

    @Override
    public void startDownload() {
        this.item.setStatus(Status.STARTED);
        stopDownloading.set(false);
        this.saveSyncWithPodcast();
        convertAndSaveBroadcast();
        if (runnableDownloader == null) {
            this.download();
        } else {
            synchronized (runnableDownloader) {
                runnableDownloader.notify();
            }
        }
    }
    
    public Boolean hasDomain(String url) {
        return url.contains("://");
    }
}
