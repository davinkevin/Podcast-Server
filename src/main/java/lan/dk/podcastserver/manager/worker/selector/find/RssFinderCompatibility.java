package lan.dk.podcastserver.manager.worker.selector.find;

import lan.dk.podcastserver.manager.worker.finder.RSSFinder;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Component
public class RSSFinderCompatibility implements FinderCompatibility<RSSFinder> {

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return Integer.MAX_VALUE-1;
    }

    @Override
    public Class<RSSFinder> finder() {
        return RSSFinder.class;
    }
}
