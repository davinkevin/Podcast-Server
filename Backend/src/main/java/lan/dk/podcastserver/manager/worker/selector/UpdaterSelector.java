package lan.dk.podcastserver.manager.worker.selector;

import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.manager.worker.updater.NoOpUpdater;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Created by kevin on 06/03/15.
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class UpdaterSelector {

    public static final NoOpUpdater NO_OP_UPDATER = new NoOpUpdater();

    final private Set<Updater> updaters;
    
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
        return updaters.stream().map(Updater::type).collect(toSet());
    }


}
