package com.github.davinkevin.podcastserver.scheduled

import com.github.davinkevin.podcastserver.service.DatabaseService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Created by kevin on 28/03/2016 for Podcast Server
 */
@Component
@ConditionalOnProperty("podcastserver.backup.enabled")
class DatabaseScheduled(val databaseService: DatabaseService) {

    @Scheduled(cron = "\${podcastserver.backup.cron:0 0 4 * * *}")
    fun backup() = databaseService.backupWithDefault()
}
