package com.github.davinkevin.podcastserver.scheduled

import com.github.davinkevin.podcastserver.business.update.UpdatePodcastBusiness
import lan.dk.podcastserver.manager.ItemDownloadManager
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Created by kevin on 26/12/2013.
 */
@Component
class UpdateScheduled(val updatePodcastBusiness: UpdatePodcastBusiness, val idm: ItemDownloadManager) {

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    @Scheduled(cron = "\${podcastserver.update-and-download.refresh.cron:0 0 * * * *}")
    fun updateAndDownloadPodcast() {
        log.info(">>> Beginning of the update <<<")
        updatePodcastBusiness.updatePodcast()
        idm.launchDownload()
        log.info(">>> End of the update <<<")
    }
}
