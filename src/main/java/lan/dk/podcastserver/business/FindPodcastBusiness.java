package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.manager.worker.finder.Finder;
import lan.dk.podcastserver.service.WorkerService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by kevin on 22/02/15.
 */
@Component
public class FindPodcastBusiness {

    @Resource
    WorkerService workerService;
    
    public Podcast fetchPodcastInfoByUrl(String url) throws FindPodcastNotFoundException {
        Finder specificFinder = workerService.getFinderByUrl(url);
        if (specificFinder == null) {
            return null;
        }

        return specificFinder.find(url);
    }
}
