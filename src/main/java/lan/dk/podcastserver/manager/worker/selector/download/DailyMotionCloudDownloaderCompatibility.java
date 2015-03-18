package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.DailyMotionCloudDownloader;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 17/03/15.
 */
@Component
public class DailyMotionCloudDownloaderCompatibility implements DownloaderCompatibility<DailyMotionCloudDownloader> {
    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("cdn.dmcloud") ? 1 : Integer.MAX_VALUE;
    }

    @Override
    public Class<DailyMotionCloudDownloader> downloader() {
        return DailyMotionCloudDownloader.class;
    }
}
