package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.BeInSportsUpdater;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 06/03/15.
 */
@Component
public class BeInSportUpdaterCompatibility implements UpdaterCompatibility<BeInSportsUpdater> {
    @Override
    public Integer compatibility(String url) {
        return url != null && url.contains("beinsports.fr")
                    ? 1 
                    : Integer.MAX_VALUE;
    }

    @Override
    public Class<BeInSportsUpdater> updater() {
        return BeInSportsUpdater.class;
    }
}
