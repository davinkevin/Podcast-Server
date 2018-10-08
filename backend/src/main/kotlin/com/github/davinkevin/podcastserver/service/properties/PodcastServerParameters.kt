package com.github.davinkevin.podcastserver.service.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZonedDateTime.now

/**
 * Created by kevin on 03/02/15.
 */
@ConfigurationProperties(value = "podcastserver")
class PodcastServerParameters(
        var rootfolder: Path = Paths.get("/tmp"),
        var coverDefaultName: String = "cover",
        var downloadExtension: String = ".psdownload",
        var maxUpdateParallels: Int = 256,
        var concurrentDownload: Int = 3,
        var numberOfTry: Int = 10,
        var numberOfDayToDownload: Long = 30L,
        var numberOfDayToSaveCover: Long = 365L,
        var rssDefaultNumberItem: Long = 50L
) {
    fun limitDownloadDate() = now().minusDays(numberOfDayToDownload)!!
    fun limitToKeepCoverOnDisk() = now().minusDays(numberOfDayToSaveCover)!!
}
