package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.CanalPlusUpdater;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 06/03/15.
 */
@Component
public class CanalPlusUpdaterCompatibility implements UpdaterCompatibility<CanalPlusUpdater> {
    
    @Override
    public Integer compatibility(String url) {
        return url != null && url.contains("canalplus.fr")
                ? 1
                : Integer.MAX_VALUE;
    }

    @Override
    public Class<CanalPlusUpdater> updater() {
        return CanalPlusUpdater.class;
    }
}
