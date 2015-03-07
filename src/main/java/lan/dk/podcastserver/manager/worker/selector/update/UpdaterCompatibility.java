package lan.dk.podcastserver.manager.worker.selector.update;

import lan.dk.podcastserver.manager.worker.updater.Updater;

/**
 * Created by kevin on 06/03/15.
 */
public interface UpdaterCompatibility<T extends Updater> {
    
    Integer compatibility(String url);
    Class<T> updater();
}
