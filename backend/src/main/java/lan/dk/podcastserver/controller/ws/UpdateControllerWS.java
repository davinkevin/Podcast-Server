package lan.dk.podcastserver.controller.ws;

import com.github.davinkevin.podcastserver.business.update.UpdatePodcastBusiness;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

/**
 * Created by kevin on 15/05/15 for HackerRank problem
 */
@Controller
public class UpdateControllerWS {

    private final UpdatePodcastBusiness updatePodcastBusiness;

    @java.beans.ConstructorProperties({"updatePodcastBusiness"})
    public UpdateControllerWS(UpdatePodcastBusiness updatePodcastBusiness) {
        this.updatePodcastBusiness = updatePodcastBusiness;
    }

    @SubscribeMapping("/updating")
    public Boolean isUpdating() {
        return updatePodcastBusiness.isUpdating();
    }
}
