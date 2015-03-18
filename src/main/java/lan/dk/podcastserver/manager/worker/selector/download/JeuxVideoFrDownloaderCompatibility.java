package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.JeuxVideoFrDownloader;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 17/03/15.
 */
@Component
public class JeuxVideoFrDownloaderCompatibility implements DownloaderCompatibility<JeuxVideoFrDownloader> {
    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("www.jeuxvideo.fr") ? 1 : Integer.MAX_VALUE;
    }

    @Override
    public Class<JeuxVideoFrDownloader> downloader() {
        return JeuxVideoFrDownloader.class;
    }
}
