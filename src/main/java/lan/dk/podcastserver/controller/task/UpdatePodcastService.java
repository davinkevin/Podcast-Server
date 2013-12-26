package lan.dk.podcastserver.controller.task;

import lan.dk.podcastserver.business.UpdatePodcastBusiness;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.annotation.Resource;


@Controller
@RequestMapping("/task/updateManager")
public class UpdatePodcastService {

    @Resource
    UpdatePodcastBusiness updatePodcastBusiness;

    @Resource
    ItemDownloadManager IDM;

    @RequestMapping(value = "/updatePodcast", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    private void updatePodcast () {
        updatePodcastBusiness.updatePodcast();
    }

    @RequestMapping(value = "/updatePodcast", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    private void updatePodcast (@RequestBody int id) {
        updatePodcastBusiness.updatePodcast(id);
    }

    @RequestMapping(value = "/updatePodcast/force", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Transactional
    private void updatePodcastForced (@RequestBody int id) {
        updatePodcastBusiness.forceUpdatePodcast(id);
    }

    @RequestMapping(value = "/updateAndDownloadPodcast", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    private void updateAndDownloadPodcast() {
        updatePodcastBusiness.updatePodcast();
        IDM.launchDownload();
    }

    @RequestMapping(value = "/deleteOdlItems", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    private void deleteOldItem() {
        updatePodcastBusiness.deleteOldEpisode();
    }
}
