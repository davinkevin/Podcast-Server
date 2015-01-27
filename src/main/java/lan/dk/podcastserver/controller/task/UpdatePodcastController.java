package lan.dk.podcastserver.controller.task;

import lan.dk.podcastserver.business.UpdatePodcastBusiness;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


@RestController
@RequestMapping("/api/task/updateManager")
public class UpdatePodcastController {

    @Resource
    UpdatePodcastBusiness updatePodcastBusiness;

    @Resource
    ItemDownloadManager IDM;

    @RequestMapping(value = "/updatePodcast", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    private void updatePodcast () {
        updatePodcastBusiness.updateAsyncPodcast();
    }

    @RequestMapping(value = "/updatePodcast", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    private void updatePodcast (@RequestBody int id) {
        updatePodcastBusiness.updatePodcast(id);
    }

    @RequestMapping(value = "/updatePodcast/force", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    private void updatePodcastForced (@RequestBody int id) {
        updatePodcastBusiness.forceUpdatePodcast(id);
    }

    @RequestMapping(value = "/updateAndDownloadPodcast", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    private void updateAndDownloadPodcast() {
        updatePodcastBusiness.updateAsyncPodcast();
        IDM.launchDownload();
    }

    @RequestMapping(value = "/deleteOdlItems", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    private void deleteOldItem() {
        updatePodcastBusiness.deleteOldEpisode();
    }
}
