package lan.dk.podcastserver.manager.worker.selector;

import javaslang.collection.HashSet;
import javaslang.collection.Set;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.manager.worker.updater.NoOpUpdater;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;

/**
 * Created by kevin on 06/03/15.
 */
@Service
@RequiredArgsConstructor
public class UpdaterSelector {

    static final NoOpUpdater NO_OP_UPDATER = new NoOpUpdater();

    private final java.util.Set<Updater> updaters;
    
    public Updater of(String url) {
        if (StringUtils.isEmpty(url)) {
            return NO_OP_UPDATER;
        }
        
        return updaters
                .stream()
                .min(Comparator.comparing(updater -> updater.compatibility(url)))
                .orElse(NO_OP_UPDATER);
    }

    public Set<AbstractUpdater.Type> types() {
        return HashSet.ofAll(updaters).map(Updater::type);
    }


}
