package lan.dk.podcastserver.manager.downloader;

import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.nio.file.Files;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 10/03/2016 for Podcast Server
 */
public class NoOpDownloader implements Downloader {

    @Setter @Getter @Accessors(chain = true) protected DownloadingItem downloadingItem;
    @Setter @Accessors(chain = true) protected ItemDownloadManager itemDownloadManager;

    @Override public Item download() { return null; }
    @Override public Item getItem() { return null; }
    @Override public String getItemUrl(Item item) { return null; }
    @Override public void startDownload() { failDownload(); }
    @Override public void pauseDownload() {}
    @Override public void restartDownload() {}
    @Override public void stopDownload() {}
    @Override public void finishDownload() {}
    @Override public void failDownload() {
        itemDownloadManager.removeACurrentDownload(downloadingItem.getItem());
    }
    @Override public Integer compatibility(DownloadingItem item) {
        return -1;
    }
    @Override public void run() { startDownload(); }
}
