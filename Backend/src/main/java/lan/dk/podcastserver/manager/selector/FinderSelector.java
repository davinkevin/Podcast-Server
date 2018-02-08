package lan.dk.podcastserver.manager.selector;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import lan.dk.podcastserver.manager.worker.Finder;
import lan.dk.podcastserver.manager.worker.noop.NoOpFinder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;

import static io.vavr.API.Option;

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Service
public class FinderSelector {

    public static final NoOpFinder NO_OP_FINDER = new NoOpFinder();

    private final Set<Finder> finders;

    public FinderSelector(java.util.Set<Finder> finders) {
        this.finders = HashSet.ofAll(finders);
    }

    public Finder of(String url) {
        return Option(url)
                .filter(StringUtils::isNotEmpty)
                .map(u -> finders)
                .getOrElse(HashSet.empty())
                .minBy(Comparator.comparing(finder -> finder.compatibility(url)))
                .getOrElse(NO_OP_FINDER);
    }
}
