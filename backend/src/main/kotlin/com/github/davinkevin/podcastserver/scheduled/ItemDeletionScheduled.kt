package com.github.davinkevin.podcastserver.scheduled

import com.github.davinkevin.podcastserver.business.update.UpdatePodcastBusiness
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Created by kevin on 26/12/2013.
 */
@Component
class ItemDeletionScheduled(val updatePodcastBusiness: UpdatePodcastBusiness) {

    @Scheduled(fixedDelay = 86400000)
    fun deleteOldItem() = updatePodcastBusiness.deleteOldEpisode()

    @Scheduled(cron = "0 0 3 * * *")
    fun deleteOldCover() = updatePodcastBusiness.deleteOldCover()
}
