package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.utils.MimeTypeUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Transactional
public abstract class AbstractDownloader implements Runnable, Downloader {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String WS_TOPIC_DOWNLOAD = "/topic/download";
    public static final String WS_TOPIC_PODCAST = "/topic/podcast/";

    protected Item item;
    protected String temporaryExtension = ".psdownload";
    protected File target = null;

    @Autowired protected ItemDownloadManager itemDownloadManager;
    @Autowired protected ItemBusiness itemService;
    @Resource protected PodcastBusiness podcastBusiness;
    @Resource protected SimpMessagingTemplate template;

    protected AtomicBoolean stopDownloading = new AtomicBoolean(false);

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public void run() {
        logger.debug("Run");
        this.startDownload();
    }

    @Override
    public void startDownload() {
        this.item.setStatus("Started");
        stopDownloading.set(false);
        this.saveSyncWithPodcast();
        this.convertAndSaveBroadcast();
        this.download();
    }

    @Override
    public void pauseDownload() {
        this.item.setStatus("Paused");
        stopDownloading.set(true);
        this.saveSyncWithPodcast();
        this.convertAndSaveBroadcast();
    }

    @Override
    public void stopDownload() {
        this.item.setStatus("Stopped");
        stopDownloading.set(true);
        this.saveSyncWithPodcast();
        itemDownloadManager.removeACurrentDownload(item);
        if (target != null && target.exists())
            target.delete();
        this.convertAndSaveBroadcast();
    }

    @Override
    public void finishDownload() {
        itemDownloadManager.removeACurrentDownload(item);
        if (target != null) {
            this.item.setStatus("Finish");
            try {

                if (target.getAbsolutePath().contains(temporaryExtension)) { // Si contient l'extention temporaire.
                    File targetWithoutExtension = new File(target.getAbsolutePath().replace(temporaryExtension, ""));
                    if (targetWithoutExtension.exists())
                        targetWithoutExtension.delete();
                    FileUtils.moveFile(target, targetWithoutExtension);
                    target = targetWithoutExtension;
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("IOException :", e);
            }

            this.item.setLocalUrl(itemDownloadManager.getFileContainer() + "/" + item.getPodcast().getTitle() + "/" + FilenameUtils.getName(String.valueOf(target)));
            this.item.setLocalUri(target.getAbsolutePath());
            this.item.setDownloaddate(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()));
            this.item.setLength(FileUtils.sizeOf(target));

            try {
                this.item.setMimeType(Files.probeContentType(target.toPath()));
                if (this.item.getMimeType() == null) {
                    this.item.setMimeType(MimeTypeUtils.getMimeType(FilenameUtils.getExtension(target.getAbsolutePath())));
                }
            } catch (IOException e) {
                e.printStackTrace();
                this.item.setMimeType(MimeTypeUtils.getMimeType(FilenameUtils.getExtension(target.getAbsolutePath())));
            }

            this.saveSyncWithPodcast();
            this.convertAndSaveBroadcast();
        } else {
            resetDownload();
        }


    }

    public void resetDownload() {
        this.stopDownload();
        //item.setStatus("Not Downloaded").setProgression(0);
        //itemService.save(item);
        //itemDownloadManager.addItemToQueue(item);
    }

    public File getTagetFile (Item item) {

        if (target != null)
            return target;

        File finalFile = new File(itemDownloadManager.getRootfolder() + File.separator + item.getPodcast().getTitle() + File.separator + FilenameUtils.getName(String.valueOf(item.getUrl())) );
        logger.debug("Création du fichier : {}", finalFile.getAbsolutePath());
        //logger.debug(file.getAbsolutePath());

        if (!finalFile.getParentFile().exists()) {
            finalFile.getParentFile().mkdirs();
        }

        if (finalFile.exists() || new File(finalFile.getAbsolutePath().concat(temporaryExtension)).exists()) {
            logger.info("Doublon sur le fichier en lien avec {} - {}, {}", item.getPodcast().getTitle(), item.getId(), item.getTitle() );
            try {
                finalFile  = File.createTempFile(
                        FilenameUtils.getBaseName(item.getUrl()).concat("-"),
                        ".".concat(FilenameUtils.getExtension(item.getUrl())),
                        finalFile.getParentFile());
                finalFile.delete();
            } catch (IOException e) {
                logger.error("Erreur lors du renommage d'un doublon", e);
            }
        }

        return new File(finalFile.getAbsolutePath() + temporaryExtension) ;
    }

    protected Item saveSyncWithPodcast() {
        try {
            this.item.setPodcast(podcastBusiness.findOne(this.item.getPodcast().getId()));
            this.item.getPodcast().getItems().remove(this.item);
            this.item.getPodcast().getItems().add(this.item);

            return itemService.save(this.item);

        } catch (Exception e) {
            logger.error("Error during saven and Sync for status {}", this.item.getStatus(), e);
            return new Item();
        }
    }

    protected void convertAndSaveBroadcast() {
        this.template.convertAndSend(WS_TOPIC_DOWNLOAD, this.item );
        this.template.convertAndSend(WS_TOPIC_PODCAST.concat(String.valueOf(item.getPodcast().getId())), this.item );
    }
}
