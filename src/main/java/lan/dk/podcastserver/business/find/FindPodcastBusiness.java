package lan.dk.podcastserver.business.find;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.manager.worker.finder.Finder;
import lan.dk.podcastserver.service.WorkerService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by kevin on 22/02/15 for Podcast Server
 */
@Component
public class FindPodcastBusiness {

    @Resource WorkerService workerService;
    
    public Podcast fetchPodcastInfoByUrl(String url) throws FindPodcastNotFoundException {
        Finder specificFinder = workerService.finderOf(url);
        if (specificFinder == null) {
            return null;
        }

        return specificFinder.find(url);
    }
}
