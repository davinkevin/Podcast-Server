package lan.dk.podcastserver.manager.worker.selector;

import lan.dk.podcastserver.manager.worker.finder.Finder;
import lan.dk.podcastserver.manager.worker.finder.NoOpFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Set;

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Service
@RequiredArgsConstructor
public class FinderSelector {

    public static final NoOpFinder NO_OP_FINDER = new NoOpFinder();

    private final Set<Finder> finders;

    public Finder of(String url) {
        if (StringUtils.isEmpty(url)) {
            return NO_OP_FINDER;
        }

        return finders
                .stream()
                .min(Comparator.comparing(updater -> updater.compatibility(url)))
                .orElse(NO_OP_FINDER);
    }
}
