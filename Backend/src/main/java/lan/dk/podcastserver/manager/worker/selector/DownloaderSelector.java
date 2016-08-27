package lan.dk.podcastserver.manager.worker.selector;

import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.downloader.NoOpDownloader;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.TargetClassAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Set;

/**
 * Created by kevin on 17/03/15.
 */
@Service
@RequiredArgsConstructor
public class DownloaderSelector {

    public static final NoOpDownloader NO_OP_DOWNLOADER = new NoOpDownloader();

    private final ApplicationContext applicationContext;
    private final Set<Downloader> downloaders;

    public Downloader of(String url) {
        if (StringUtils.isEmpty(url)) {
            return NO_OP_DOWNLOADER;
        }

        return downloaders
                .stream()
                .min(Comparator.comparing(downloader -> downloader.compatibility(url)))
                .map(d -> TargetClassAware.class.isInstance(d) ? TargetClassAware.class.cast(d).getTargetClass() : d.getClass())
                .map(Class::getSimpleName)
                .map(clazz -> applicationContext.getBean(clazz, Downloader.class))
                .orElse(NO_OP_DOWNLOADER);
    }
}
