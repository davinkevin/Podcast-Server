package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.RSSUpdater;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 06/03/15.
 */
@Component
public class RssUpdaterCompatibility implements UpdaterCompatibility<RSSUpdater> {

    @Override
    public Integer compatibility(String url) {
        return Integer.MAX_VALUE-1;
    }

    @Override
    public Class<RSSUpdater> updater() {
        return RSSUpdater.class;
    }


}
