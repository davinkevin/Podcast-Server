package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.business.update.UpdatePodcastBusiness;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 26/12/2013.
 */
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UpdateScheduled {

    final UpdatePodcastBusiness updatePodcastBusiness;
    final ItemDownloadManager IDM;

    @Scheduled(cron="${podcastserver.update-and-download.refresh.cron:0 0 * * * *}")
    public void updateAndDownloadPodcast() {
        log.info(">>> Beginning of the update <<<");
        updatePodcastBusiness.updatePodcast();
        IDM.launchDownload();
        log.info(">>> End of the update <<<");
    }
}
