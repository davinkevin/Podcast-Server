package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;

/**
 * Created by kevin on 10/03/2016 for Podcast Server
 */
public class NoOpDownloader implements Downloader {
    @Override
    public Item download() {
        return null;
    }

    @Override
    public Downloader setItem(Item item) {
        return null;
    }

    @Override
    public Item getItem() {
        return null;
    }

    @Override
    public String getItemUrl() {
        return null;
    }

    @Override
    public void startDownload() {}

    @Override
    public void pauseDownload() {}

    @Override
    public void stopDownload() {}

    @Override
    public void finishDownload() {}

    @Override
    public void resetDownload() {}

    @Override
    public Integer compatibility(String url) {
        return null;
    }

    @Override
    public void run() {}
}
