package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.DailymotionDownloader;
import org.springframework.stereotype.Component;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Component
public class DailymotionDownloaderCompatibility implements DownloaderCompatibility<DailymotionDownloader> {

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("dailymotion.com/video") ? 1 : Integer.MAX_VALUE;
    }

    @Override
    public Class<DailymotionDownloader> downloader() {
        return DailymotionDownloader.class;
    }
}
