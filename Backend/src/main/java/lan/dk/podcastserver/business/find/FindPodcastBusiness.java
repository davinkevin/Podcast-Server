package lan.dk.podcastserver.business.find;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.selector.FinderSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 22/02/15 for Podcast Server
 */
@Component
@RequiredArgsConstructor
public class FindPodcastBusiness {

    private final FinderSelector finderSelector;
    
    public Podcast fetchPodcastInfoByUrl(String url) {
        return finderSelector.of(url).find(url);
    }
}
