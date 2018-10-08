package com.github.davinkevin.podcastserver.service.properties

import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.time.ZonedDateTime.now

/**
 * Created by kevin on 13/07/15 for Podcast Server
 */
class PodcastServerParametersTest {

    @Test
    fun should_have_default_value() {
        val parameters = PodcastServerParameters()

        assertThat(parameters.rootfolder).isEqualTo(Paths.get("/tmp/"))
        assertThat(parameters.coverDefaultName).isEqualTo("cover")
        assertThat(parameters.downloadExtension).isEqualTo(".psdownload")
        assertThat(parameters.concurrentDownload).isEqualTo(3)
        assertThat(parameters.numberOfTry).isEqualTo(10)
        assertThat(parameters.rssDefaultNumberItem).isEqualTo(50L)
        assertThat(parameters.numberOfDayToDownload).isEqualTo(30L)
        assertThat(parameters.numberOfDayToSaveCover).isEqualTo(365L)
        assertThat(parameters.limitToKeepCoverOnDisk())
                .isAfter(now().minusYears(1).minusDays(10))
                .isBefore(now().minusDays(364))
    }

    @Test
    fun should_have_modified_values() {
        /* Given */
        val rootFolder = Paths.get("/tmp/bar")
        val parameters = PodcastServerParameters()

        /* When */
        parameters.rootfolder = rootFolder
        parameters.coverDefaultName = "default"
        parameters.downloadExtension = ".bardownload"
        parameters.maxUpdateParallels = 5
        parameters.concurrentDownload = 5
        parameters.numberOfTry = 20
        parameters.numberOfDayToDownload = 5L
        parameters.numberOfDayToSaveCover = 5L
        parameters.rssDefaultNumberItem = 25L

        /* Then */
        assertThat(parameters.downloadExtension).isEqualTo(".bardownload")
        assertThat(parameters.rootfolder).isEqualTo(rootFolder)
        assertThat(parameters.coverDefaultName).isEqualTo("default")
        assertThat(parameters.concurrentDownload).isEqualTo(5)
        assertThat(parameters.numberOfTry).isEqualTo(20)
        assertThat(parameters.rssDefaultNumberItem).isEqualTo(25L)
        assertThat(parameters.limitDownloadDate())
                .isBeforeOrEqualTo(now().minusDays(parameters.numberOfDayToDownload))
                .isAfterOrEqualTo(now().minusDays(parameters.numberOfDayToDownload).minusMinutes(1))
    }

}
