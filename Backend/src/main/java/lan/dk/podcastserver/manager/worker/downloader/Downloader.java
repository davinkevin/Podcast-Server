package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;

public interface Downloader extends Runnable{

    Item download();

    Downloader setItem(Item item);
    Downloader setItemDownloadManager(ItemDownloadManager itemDownloadManager);

    Item getItem();
    String getItemUrl(Item item);

    void startDownload();

    void pauseDownload();
    default void restartDownload() { this.startDownload(); }

    void stopDownload();

    void finishDownload();
    void resetDownload();

    Integer compatibility(String url);

}
