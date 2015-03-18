package lan.dk.podcastserver.manager.worker.selector.download;

import lan.dk.podcastserver.manager.worker.downloader.HTTPDownloader;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 17/03/15.
 */
@Component
public class HTTPDownloaderCompatibility implements DownloaderCompatibility<HTTPDownloader> {

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return url.startsWith("http") ? Integer.MAX_VALUE-1 : Integer.MAX_VALUE;
    }

    @Override
    public Class<HTTPDownloader> downloader() {
        return HTTPDownloader.class;
    }
}
