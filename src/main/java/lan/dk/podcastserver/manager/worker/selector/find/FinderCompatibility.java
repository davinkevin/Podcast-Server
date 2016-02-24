package lan.dk.podcastserver.manager.worker.selector.find;

import lan.dk.podcastserver.manager.worker.finder.Finder;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
public interface FinderCompatibility<T extends Finder> {
    Integer compatibility(@NotEmpty String url);
    Class<T> finder();
}
