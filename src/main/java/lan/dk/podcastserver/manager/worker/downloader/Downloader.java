package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;

public interface Downloader {

    public Item download();

    public Downloader setItem(Item item);
    public Item getItem();
    public String getItemUrl();

    public void startDownload();
    public void pauseDownload();
    public void stopDownload();
    public void finishDownload();
    public void resetDownload();

}
