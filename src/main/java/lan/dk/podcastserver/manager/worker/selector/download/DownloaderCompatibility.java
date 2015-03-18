package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by kevin on 17/03/15.
 */
public interface DownloaderCompatibility<T extends Downloader> {

    Integer compatibility(@NotEmpty String url);
    Class<T> downloader();
}
