package com.github.davinkevin.podcastserver.service.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZonedDateTime.now

/**
 * Created by kevin on 03/02/15.
 */
@ConstructorBinding
@ConfigurationProperties(value = "podcastserver")
data class PodcastServerParameters(
        val maxUpdateParallels: Int = 256,
        val concurrentDownload: Int = 3,
        val numberOfTry: Int = 10,
        val numberOfDayToDownload: Long = 30L,
        val numberOfDayToSaveCover: Long = 365L,
        val rssDefaultNumberItem: Long = 50L
) {
    fun limitDownloadDate() = now().minusDays(numberOfDayToDownload)!!
    fun limitToKeepCoverOnDisk() = now().minusDays(numberOfDayToSaveCover)!!
}
