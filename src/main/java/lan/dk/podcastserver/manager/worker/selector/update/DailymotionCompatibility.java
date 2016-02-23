package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.DailymotionUpdater;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 21/02/2016 for Podcast Server
 */
@Component
public class DailymotionCompatibility implements UpdaterCompatibility<DailymotionUpdater> {

    @Override
    public Integer compatibility(String url) {
        return nonNull(url) && url.contains("www.dailymotion.com")
                ? 1
                : Integer.MAX_VALUE;
    }

    @Override
    public Class<DailymotionUpdater> updater() {
        return DailymotionUpdater.class;
    }
}
