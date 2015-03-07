package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.ParleysUpdater;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 07/03/15.
 */
@Component
public class ParleysCompatibility implements UpdaterCompatibility<ParleysUpdater>{

    @Override
    public Integer compatibility(String url) {
        return url != null && url.contains("parleys.com")
                ? 1
                : Integer.MAX_VALUE;
    }

    @Override
    public Class<ParleysUpdater> updater() {
        return ParleysUpdater.class;
    }

}
