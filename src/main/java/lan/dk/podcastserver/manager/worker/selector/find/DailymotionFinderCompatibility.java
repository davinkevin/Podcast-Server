package lan.dk.podcastserver.manager.worker.selector.find;

import lan.dk.podcastserver.manager.worker.finder.DailymotionFinder;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Component
public class DailymotionFinderCompatibility implements FinderCompatibility<DailymotionFinder>{

    @Override
    public Integer compatibility(@NotEmpty String url) {
        return nonNull(url) && url.contains("www.dailymotion.com")
                ? 1
                : Integer.MAX_VALUE;
    }

    @Override
    public Class<DailymotionFinder> finder() { return DailymotionFinder.class; }
}
