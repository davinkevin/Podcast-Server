package lan.dk.podcastserver.controller.task;

import com.github.davinkevin.podcastserver.business.update.UpdatePodcastBusiness;
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/task/updateManager")
public class UpdatePodcastController {

    private final UpdatePodcastBusiness updatePodcastBusiness;
    private final ItemDownloadManager IDM;

    @java.beans.ConstructorProperties({"updatePodcastBusiness", "IDM"})
    public UpdatePodcastController(UpdatePodcastBusiness updatePodcastBusiness, ItemDownloadManager IDM) {
        this.updatePodcastBusiness = updatePodcastBusiness;
        this.IDM = IDM;
    }

    @RequestMapping(value = "/updatePodcast", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void updatePodcast () {
        updatePodcastBusiness.updatePodcast();
    }

    @RequestMapping(value = "/updateAndDownloadPodcast", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void updateAndDownloadPodcast() {
        updatePodcastBusiness.updatePodcast();
        IDM.launchDownload();
    }
}
