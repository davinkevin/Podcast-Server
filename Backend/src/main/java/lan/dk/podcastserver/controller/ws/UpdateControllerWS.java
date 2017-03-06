package lan.dk.podcastserver.controller.ws;

import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

/**
 * Created by kevin on 15/05/15 for HackerRank problem
 */
@Controller
@RequiredArgsConstructor
public class UpdateControllerWS {

    private final UpdatePodcastBusiness updatePodcastBusiness;

    @SubscribeMapping("/updating")
    public Boolean isUpdating() {
        return updatePodcastBusiness.isUpdating();
    }
}
