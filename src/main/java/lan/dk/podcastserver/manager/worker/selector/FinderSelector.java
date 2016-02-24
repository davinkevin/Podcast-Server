package lan.dk.podcastserver.manager.worker.selector;

import lan.dk.podcastserver.manager.worker.selector.find.FinderCompatibility;
import lombok.RequiredArgsConstructor;
import org.jadira.usertype.spi.utils.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Set;

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class FinderSelector {

    final Set<FinderCompatibility> finderCompatibilities;

    public Class of(String url) {
        if (StringUtils.isEmpty(url)) {
            throw new RuntimeException();
        }

        return finderCompatibilities
                .stream()
                .min(Comparator.comparing(updater -> updater.compatibility(url)))
                .get()
                .finder();
    }
}
