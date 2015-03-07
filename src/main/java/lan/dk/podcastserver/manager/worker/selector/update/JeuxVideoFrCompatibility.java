package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.JeuxVideoFRUpdater;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 07/03/15.
 */
@Component
public class JeuxVideoFrCompatibility implements UpdaterCompatibility<JeuxVideoFRUpdater>{

    @Override
    public Integer compatibility(String url) {
        return url != null && url.contains("jeuxvideo.fr")
                ? 1
                : Integer.MAX_VALUE;
    }

    @Override
    public Class<JeuxVideoFRUpdater> updater() {
        return JeuxVideoFRUpdater.class;
    }

}
