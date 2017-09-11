package lan.dk.podcastserver.manager.worker.downloader;

import io.vavr.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import static io.vavr.API.Some;

public interface Downloader extends Runnable {

    Item download();

    Downloader setItem(Item item);
    Downloader setItemDownloadManager(ItemDownloadManager itemDownloadManager);

    Item getItem();
    String getItemUrl(Item item);
    default String getFileName(Item item) {
        return Some(getItemUrl(item))
                .map(s -> StringUtils.substringBefore(s, "?"))
                .map(FilenameUtils::getName)
                .getOrElse("");
    };

    void startDownload();

    void pauseDownload();
    default void restartDownload() { this.startDownload(); }
    void stopDownload();
    void finishDownload();
    void resetDownload();

    Integer compatibility(String url);

}
