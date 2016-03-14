package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;

public interface Downloader extends Runnable{

    Item download();

    Downloader setItem(Item item);
    Item getItem();
    String getItemUrl();

    void startDownload();
    void pauseDownload();
    void stopDownload();
    void finishDownload();
    void resetDownload();

    Integer compatibility(String url);

}
