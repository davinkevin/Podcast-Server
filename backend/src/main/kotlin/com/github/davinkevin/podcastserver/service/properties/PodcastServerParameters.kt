package com.github.davinkevin.podcastserver.service.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.OffsetDateTime
import java.time.ZonedDateTime.now

/**
 * Created by kevin on 03/02/15.
 */
@ConfigurationProperties(value = "podcastserver")
data class PodcastServerParameters(
        val maxUpdateParallels: Int = 256,
        val concurrentDownload: Int = 3,
        val numberOfTry: Int = 10,
        val numberOfDayToDownload: Long = 30L,
        val numberOfDayToSaveCover: Long = 365L,
        val rssDefaultNumberItem: Long = 50L
) {
    fun limitDownloadDate() = OffsetDateTime.now().minusDays(numberOfDayToDownload)!!
    fun limitToKeepCoverOnDisk() = now().minusDays(numberOfDayToSaveCover)!!
}
