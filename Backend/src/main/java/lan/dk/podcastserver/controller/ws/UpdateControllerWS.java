package lan.dk.podcastserver.controller.ws;

import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;

/**
 * Created by kevin on 15/05/15 for HackerRank problem
 */
@Controller
public class UpdateControllerWS {

    @Resource UpdatePodcastBusiness updatePodcastBusiness;

    @SubscribeMapping("/updating")
    public Boolean isUpdating() {
        return updatePodcastBusiness.isUpdating();
    }
}
