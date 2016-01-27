package lan.dk.podcastserver.manager.worker.downloader;

import com.google.common.io.ByteStreams;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toList;

@Scope("prototype")
@Component("M3U8Downloader")
public class M3U8Downloader extends AbstractDownloader {

    @Autowired UrlService urlService;

    List<String> urlList;
    final M3U8Watcher watcher = new M3U8Watcher(this);

    @Override
    public Item download() {
        logger.debug("Download");

        if (watcher.hasBeenStarted().get()) {
            synchronized (watcher) {
                watcher.notify();
            }
            return item;
        }

        itemDownloadManager.addACurrentDownload();

        if (!getItemUrl().contains("m3u8")) {
            logger.debug("Traiter avec le downloader adapté à {}", getItemUrl());
            stopDownload();
            return item;
        }

        try(BufferedReader in = urlService.urlAsReader(getItemUrl())) {
            urlList = in
                    .lines()
                    .filter(l -> !l.startsWith("#"))
                    .map(l -> urlService.urlWithDomain(getItemUrl(), l))
                    .collect(toList());

            // Tous les éléments sont disponibles dans urlList :
            target = getTagetFile(item);

            watcher.run();
        } catch (IOException e) {
            logger.error("Error during fetching individual url of M3U8", e);
            stopDownload();
        }

        return this.item;
    }

    @Override
    public File getTagetFile (Item item) {

        if (target != null)
            return target;

        String fileNameFromM3U8Playlist = urlService.getFileNameM3U8Url(item.getUrl());

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
        download();
    }

    @Slf4j
    static class M3U8Watcher implements Runnable {

        private final M3U8Downloader downloader;;
        private List<String> urlList;
        private UrlService urlService;
        private Item item;

        private AtomicBoolean hasBeenStarted = new AtomicBoolean(false);

        public M3U8Watcher(M3U8Downloader downloader) {
            this.downloader = downloader;
        }

        @Override
        public void run() {
            start();
            item = downloader.item;
            urlList = downloader.urlList;
            urlService = downloader.urlService;

            try(OutputStream outputStream = new FileOutputStream(downloader.target)) {

                for (int cpt = 0; cpt < urlList.size(); cpt++) {
                    String urlFragmentToDownload = urlList.get(cpt);

                    log.debug("URL : {}", urlFragmentToDownload);
                    try(InputStream is = urlService.getConnection(urlFragmentToDownload).getInputStream()) {
                        ByteStreams.copy(is, outputStream);
                        broadcastProgression(cpt);
                    }

                    if (downloader.stopDownloading.get()) {
                        if (Status.STOPPED == item.getStatus()) break;

                        if (Status.PAUSED == item.getStatus())
                            synchronized(this) { wait(); }
                    }
                }

                if (Status.STARTED == item.getStatus()) {
                    downloader.finishDownload();
                }
            } catch (IOException | InterruptedException e) {
                log.error("Error during runner of M3U8Downloader", e);
                downloader.stopDownload();
            }
        }

        private void broadcastProgression(int cpt) {
            item.setProgression((100*cpt)/ urlList.size());
            log.debug("Progression : {}", item.getProgression());
            downloader.convertAndSaveBroadcast();
        }

        public AtomicBoolean hasBeenStarted() {
            return hasBeenStarted;
        }

        private void start() {
            hasBeenStarted.set(true);
        }
    }

}
