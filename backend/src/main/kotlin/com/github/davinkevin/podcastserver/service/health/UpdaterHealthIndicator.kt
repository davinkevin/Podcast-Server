package com.github.davinkevin.podcastserver.service.health

import com.github.davinkevin.podcastserver.business.update.UpdatePodcastBusiness
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.stereotype.Component

/**
 * Created by kevin on 18/07/2016.
 */
@Component
class UpdaterHealthIndicator(val updater: UpdatePodcastBusiness) : AbstractHealthIndicator() {

    @Throws(Exception::class)
    override fun doHealthCheck(builder: Health.Builder) {
        // @formatter:off
        builder.up()
            .withDetail("lastFullUpdate", updater.lastFullUpdate ?: "none")
            .withDetail("isUpdating", updater.isUpdating)
            .withDetail("activeThread", updater.updaterActiveCount)
        .build()
         // @formatter:on
    }
}
