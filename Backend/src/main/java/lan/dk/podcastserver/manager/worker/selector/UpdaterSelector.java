package lan.dk.podcastserver.manager.worker.selector;

import javaslang.collection.HashSet;
import javaslang.collection.Set;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.manager.worker.updater.NoOpUpdater;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;

/**
 * Created by kevin on 06/03/15.
 */
@Service
public class UpdaterSelector {

    static final NoOpUpdater NO_OP_UPDATER = new NoOpUpdater();

    private final Set<Updater> updaters;

    public UpdaterSelector(java.util.Set<Updater> updaters) {
        this.updaters = HashSet.ofAll(updaters);
    }

    public Updater of(String url) {
        if (StringUtils.isEmpty(url)) {
            return NO_OP_UPDATER;
        }
        
        return updaters
                .minBy(Comparator.comparing(updater -> updater.compatibility(url)))
                .getOrElse(NO_OP_UPDATER);
    }

    public Set<AbstractUpdater.Type> types() {
        return updaters.map(Updater::type);
    }


}
