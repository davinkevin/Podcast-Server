package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.wget.info.DownloadInfo;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.service.api.ItemService;
import lan.dk.podcastserver.service.api.PodcastService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;


public abstract class AbstractDownloader implements Runnable, Downloader {
    @Autowired
    protected ItemDownloadManager itemDownloadManager;
    protected Item item;
    protected String temporaryExtension = ".psdownload";
    protected File target = null;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected PodcastService podcastService;

    protected DownloadInfo info = null;
    protected AtomicBoolean stopDownloading = new AtomicBoolean(false);

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    public AbstractDownloader() {
    }

    public ItemDownloadManager getItemDownloadManager() {
        return itemDownloadManager;
    }

    public void setItemDownloadManager(ItemDownloadManager itemDownloadManager) {
        this.itemDownloadManager = itemDownloadManager;
    }

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
        stopDownloading.set(false);
        this.item.setStatus("Started");
        itemService.update(this.item);
        this.download();

    }

    @Override
    public void pauseDownload() {
        stopDownloading.set(true);
        this.item.setStatus("Paused");
        itemService.update(this.item, this.item.getId());
    }

    @Override
    public void stopDownload() {
        stopDownloading.set(true);
        this.item.setStatus("Stopped");
        itemService.update(this.item, this.item.getId());
        itemDownloadManager.removeACurrentDownload(item);
        if (target != null && target.exists())
            target.delete();
    }

    @Override
    public void finishDownload() {
        itemDownloadManager.removeACurrentDownload(item);
        if (target != null) {
            this.item.setStatus("Finish");
            try {
                File targetWithoutExtension = new File(target.getAbsolutePath().replace(temporaryExtension, ""));
                if (targetWithoutExtension.exists())
                    targetWithoutExtension.delete();
                FileUtils.moveFile(target, targetWithoutExtension);
                target = targetWithoutExtension;
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("IOException :", e);
            }

            this.item.setLocalUrl(itemDownloadManager.getFileContainer() + "/" + item.getPodcast().getTitle() + "/" + FilenameUtils.getName(String.valueOf(target)));
            this.item.setLocalUri(target.getAbsolutePath());
            this.item.setDownloaddate(new Timestamp(new Date().getTime()));

            itemService.update(this.item, this.item.getId());
        } else {
            resetDownload();
        }


    }

    public void resetDownload() {
        item.setStatus("Not Downloaded");
        itemService.update(item);
        itemDownloadManager.addItemToQueue(item);
    }

    public File getTagetFile (Item item) {
        File file = new File(itemDownloadManager.getRootfolder() + File.separator + item.getPodcast().getTitle() + File.separator + FilenameUtils.getName(String.valueOf(item.getUrl())) + temporaryExtension );
        logger.debug("Création du fichier : " + itemDownloadManager.getRootfolder() + File.separator + item.getPodcast().getTitle() + File.separator + FilenameUtils.getName(String.valueOf(item.getUrl())) + temporaryExtension );
        //logger.debug(file.getAbsolutePath());
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }


}
