package lan.dk.podcastserver.controller.task;

import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/task/updateManager")
public class UpdatePodcastController {

    private final UpdatePodcastBusiness updatePodcastBusiness;
    private final ItemDownloadManager IDM;

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

    @RequestMapping(value = "/deleteOdlItems", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteOldItem() {
        updatePodcastBusiness.deleteOldEpisode();
    }
}