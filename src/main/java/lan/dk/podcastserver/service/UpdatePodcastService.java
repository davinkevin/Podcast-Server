package lan.dk.podcastserver.service;

import lan.dk.podcastserver.controller.UpdatePodcastController;
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
    UpdatePodcastController updatePodcastController;

    @Resource
    ItemDownloadManager IDM;

    @RequestMapping(value = "/updatePodcast", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Transactional
    private void updatePodcast () {
        updatePodcastController.updatePodcast();
    }

    @RequestMapping(value = "/updatePodcast", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Transactional
    private void updatePodcast (@RequestBody int id) {
        updatePodcastController.updatePodcast(id);
    }

    @RequestMapping(value = "/updatePodcast/force", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Transactional
    private void updatePodcastForced (@RequestBody int id) {
        updatePodcastController.forceUpdatePodcast(id);
    }

    //@Scheduled(cron="${updateAndDownload.refresh.cron}")
    @Scheduled(fixedDelay = 3600000)
    @RequestMapping(value = "/updateAndDownloadPodcast", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Transactional
    private void updateAndDownloadPodcast() {
        updatePodcastController.updatePodcast();
        IDM.launchDownload();
    }

/*    @Scheduled(fixedDelay = 86400000)
    @RequestMapping(value = "/updateAndDownloadPodcast", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @Transactional
    private void deleteOldItem() {
        updatePodcastController.deleteOldEpisode();
    }
  */
}
