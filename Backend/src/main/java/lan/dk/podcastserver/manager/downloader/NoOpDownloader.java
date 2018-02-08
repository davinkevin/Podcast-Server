package lan.dk.podcastserver.manager.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;

/**
 * Created by kevin on 10/03/2016 for Podcast Server
 */
public class NoOpDownloader implements Downloader {

    @Override public Item download() { return null; }
    @Override public Downloader setDownloadingItem(DownloadingItem item) { return this; }
    @Override public Downloader setItemDownloadManager(ItemDownloadManager itemDownloadManager) {
        return this;
    }
    @Override public Item getItem() { return null; }
    @Override public String getItemUrl(Item item) { return null; }
    @Override public void startDownload() {}
    @Override public void pauseDownload() {}
    @Override public void stopDownload() {}
    @Override public void finishDownload() {}
    @Override public void failDownload() {}
    @Override public Integer compatibility(String url) {
        return -1;
    }
    @Override public void run() {}
}
