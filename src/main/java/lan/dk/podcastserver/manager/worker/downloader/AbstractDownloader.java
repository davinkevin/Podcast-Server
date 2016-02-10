package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public abstract class AbstractDownloader implements Runnable, Downloader {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String WS_TOPIC_DOWNLOAD = "/topic/download";
    public static final String WS_TOPIC_PODCAST = "/topic/podcast/%s";

    protected Item item;
    protected String temporaryExtension;
    protected File target = null;

    @Resource protected PodcastRepository podcastRepository;
    @Resource protected ItemRepository itemRepository;
    @Resource protected ItemDownloadManager itemDownloadManager;
    @Resource protected PodcastServerParameters podcastServerParameters;
    @Resource protected SimpMessagingTemplate template;
    @Resource protected MimeTypeService mimeTypeService;

    protected AtomicBoolean stopDownloading = new AtomicBoolean(false);

    public Item getItem() {
        return item;
    }
    public Downloader setItem(Item item) {
        this.item = item;
        return this;
    }

    @Override
    public void run() {
        logger.debug("Run");
        startDownload();
    }

    @Override
    public void startDownload() {
        item.setStatus(Status.STARTED);
        stopDownloading.set(false);
        saveSyncWithPodcast();
        convertAndSaveBroadcast();
        download();
    }

    @Override
    public void pauseDownload() {
        item.setStatus(Status.PAUSED);
        stopDownloading.set(true);
        saveSyncWithPodcast();
        convertAndSaveBroadcast();
    }

    @Override
    public void stopDownload() {
        item.setStatus(Status.STOPPED);
        stopDownloading.set(true);
        saveSyncWithPodcast();
        itemDownloadManager.removeACurrentDownload(item);
        if (nonNull(target) && target.exists())
            target.delete();
        convertAndSaveBroadcast();
    }

    @Override
    @Transactional
    public void finishDownload() {
        itemDownloadManager.removeACurrentDownload(item);

        if (isNull(target)) {
            resetDownload();
            return;
        }

        item.setStatus(Status.FINISH);
        try {

            if (target.getAbsolutePath().contains(temporaryExtension)) {
                File targetWithoutExtension = new File(target.getAbsolutePath().replace(temporaryExtension, ""));
                if (targetWithoutExtension.exists()) targetWithoutExtension.delete();
                FileUtils.moveFile(target, targetWithoutExtension);
                target = targetWithoutExtension;
            }
            item
                .setLength(Files.size(target.toPath()))
                .setMimeType(mimeTypeService.probeContentType(target.toPath()));
        } catch (IOException e) {
            logger.error("IOException :", e);
            item.setMimeType(mimeTypeService.getMimeType(FilenameUtils.getExtension(target.getAbsolutePath())));
        }

        item.setFileName(FilenameUtils.getName(target.getAbsolutePath()));
        item.setDownloadDate(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()));

        saveSyncWithPodcast();
        convertAndSaveBroadcast();
    }

    public void resetDownload() {
        stopDownload();
    }

    @Transactional
    public File getTagetFile (Item item) {

        if (target != null)
            return target;

        File finalFile = getDestinationFile(item);
        logger.debug("Création du fichier : {}", finalFile.getAbsolutePath());

        if (!finalFile.getParentFile().exists()) {
            finalFile.getParentFile().mkdirs();
        }

        if (finalFile.exists() || new File(finalFile.getAbsolutePath().concat(temporaryExtension)).exists()) {
            logger.info("Doublon sur le fichier en lien avec {} - {}, {}", item.getPodcast().getTitle(), item.getId(), item.getTitle() );
            try {
                finalFile  = File.createTempFile(
                        FilenameUtils.getBaseName(getItemUrl()).concat("-"),
                        ".".concat(FilenameUtils.getExtension(getItemUrl())),
                        finalFile.getParentFile());
                finalFile.delete();
            } catch (IOException e) {
                logger.error("Erreur lors du renommage d'un doublon", e);
                stopDownload();
            }
        }

        return new File(finalFile.getAbsolutePath() + temporaryExtension) ;
    }

    private File getDestinationFile(Item item) {
        String fileName = FilenameUtils.getName(String.valueOf(getItemUrl()));

        if (StringUtils.contains(fileName, "?"))
            fileName = fileName.substring(0, fileName.lastIndexOf("?"));

        return  Paths.get(itemDownloadManager.getRootfolder(), item.getPodcast().getTitle(), fileName).toFile();
    }

    @Transactional
    protected void saveSyncWithPodcast() {
        try {
            item.setPodcast(podcastRepository.findOne(item.getPodcast().getId()));
            itemRepository.save(item);
        } catch (Exception e) {
            logger.error("Error during save and Sync of the item {}", item, e);
        }
    }

    @Transactional
    protected void convertAndSaveBroadcast() {
        template.convertAndSend(WS_TOPIC_DOWNLOAD, item);
        template.convertAndSend(String.format(WS_TOPIC_PODCAST, item.getPodcast().getId()), item);
    }

    public String getItemUrl() {
        return item.getUrl();
    }

    @PostConstruct
    public void postConstruct() {
        temporaryExtension = podcastServerParameters.getDownloadExtension();
    }
}
