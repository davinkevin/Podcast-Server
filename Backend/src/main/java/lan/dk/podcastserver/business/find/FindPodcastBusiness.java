package lan.dk.podcastserver.business.find;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.manager.worker.selector.FinderSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 22/02/15 for Podcast Server
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FindPodcastBusiness {

    final FinderSelector finderSelector;
    
    public Podcast fetchPodcastInfoByUrl(String url) throws FindPodcastNotFoundException {
        return finderSelector.of(url).find(url);
    }
}
