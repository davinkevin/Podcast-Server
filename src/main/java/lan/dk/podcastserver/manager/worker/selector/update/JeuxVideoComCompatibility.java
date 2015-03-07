package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.JeuxVideoComUpdater;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 07/03/15.
 */
@Component
public class JeuxVideoComCompatibility implements UpdaterCompatibility<JeuxVideoComUpdater>{

    @Override
    public Integer compatibility(String url) {
        return url != null && url.contains("jeuxvideo.com")
                ? 1
                : Integer.MAX_VALUE;
    }

    @Override
    public Class<JeuxVideoComUpdater> updater() {
        return JeuxVideoComUpdater.class;
    }

}
