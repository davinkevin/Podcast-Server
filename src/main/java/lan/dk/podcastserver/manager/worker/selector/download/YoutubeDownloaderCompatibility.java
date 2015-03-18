package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.YoutubeDownloader;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 17/03/15.
 */
@Component
public class YoutubeDownloaderCompatibility implements DownloaderCompatibility<YoutubeDownloader> {
    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("www.youtube.com") ? 1 : Integer.MAX_VALUE;
    }

    @Override
    public Class<YoutubeDownloader> downloader() {
        return YoutubeDownloader.class;
    }
}
