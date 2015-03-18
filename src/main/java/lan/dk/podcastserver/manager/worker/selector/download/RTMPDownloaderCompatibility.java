package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.RTMPDownloader;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 17/03/15.
 */
@Component
public class RTMPDownloaderCompatibility implements DownloaderCompatibility<RTMPDownloader> {
    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.startsWith("rtmp") ? 1 : Integer.MAX_VALUE;
    }

    @Override
    public Class<RTMPDownloader> downloader() {
        return RTMPDownloader.class;
    }
}
