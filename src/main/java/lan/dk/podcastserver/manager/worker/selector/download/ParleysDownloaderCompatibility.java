package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.ParleysDownloader;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 17/03/15.
 */
@Component
public class ParleysDownloaderCompatibility implements DownloaderCompatibility<ParleysDownloader>{

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.contains("parleys") ? 1 : Integer.MAX_VALUE;
    }

    @Override
    public Class<ParleysDownloader> downloader() {
        return ParleysDownloader.class;
    }
}
