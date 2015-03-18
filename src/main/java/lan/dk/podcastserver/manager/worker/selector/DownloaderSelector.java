package lan.dk.podcastserver.manager.worker.selector;

import lan.dk.podcastserver.manager.worker.selector.download.DownloaderCompatibility;
import org.jadira.usertype.spi.utils.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.Set;

/**
 * Created by kevin on 17/03/15.
 */
@Service
public class DownloaderSelector {

    Set<DownloaderCompatibility> downloaderCompatibilities;

    public Class of(String url) {
        if (StringUtils.isEmpty(url)) {
            throw new RuntimeException();
        }

        return downloaderCompatibilities
                .stream()
                .min(Comparator.comparing(downloader -> downloader.compatibility(url)))
                .get()
                .downloader();
    }

    @Resource
    public DownloaderSelector setDownloaderCompatibilities(Set<DownloaderCompatibility> downloaderCompatibilities) {
        this.downloaderCompatibilities = downloaderCompatibilities;
        return this;
    }
}
