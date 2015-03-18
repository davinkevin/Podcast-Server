package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.M3U8Downloader;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 17/03/15.
 */
@Component
public class M3U8DownloaderCompatibility implements DownloaderCompatibility<M3U8Downloader> {
    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("m3u8") ? 10 : Integer.MAX_VALUE;
    }

    @Override
    public Class<M3U8Downloader> downloader() {
        return M3U8Downloader.class;
    }
}
