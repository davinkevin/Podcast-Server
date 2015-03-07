package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.PluzzUpdater;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 07/03/15.
 */
@Component
public class PluzzCompatibility implements UpdaterCompatibility<PluzzUpdater>{

    @Override
    public Integer compatibility(String url) {
        return url != null && url.contains("pluzz.francetv.fr")
                ? 1
                : Integer.MAX_VALUE;
    }

    @Override
    public Class<PluzzUpdater> updater() {
        return PluzzUpdater.class;
    }

}
