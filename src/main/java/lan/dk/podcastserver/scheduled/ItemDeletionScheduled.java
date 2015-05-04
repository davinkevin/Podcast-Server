package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by kevin on 26/12/2013.
 */
@Component
public class ItemDeletionScheduled {

    private @Resource UpdatePodcastBusiness updatePodcastBusiness;

    @Scheduled(fixedDelay = 86400000)
    private void deleteOldItem() {
        updatePodcastBusiness.deleteOldEpisode();
    }
    
}
