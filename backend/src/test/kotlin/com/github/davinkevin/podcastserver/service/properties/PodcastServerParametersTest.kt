package com.github.davinkevin.podcastserver.service.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime.now

/**
 * Created by kevin on 13/07/15 for Podcast Server
 */
class PodcastServerParametersTest {

    @Test
    fun should_have_default_value() {
        val parameters = PodcastServerParameters()

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
        val parameters = PodcastServerParameters(
            maxUpdateParallels = 5,
            concurrentDownload = 5,
            numberOfTry = 20,
            numberOfDayToDownload = 5L,
            numberOfDayToSaveCover = 5L,
            rssDefaultNumberItem = 25L
        )

        /* When */

        /* Then */
        assertThat(parameters.concurrentDownload).isEqualTo(5)
        assertThat(parameters.numberOfTry).isEqualTo(20)
        assertThat(parameters.rssDefaultNumberItem).isEqualTo(25L)
        assertThat(parameters.limitDownloadDate())
                .isBeforeOrEqualTo(now().minusDays(parameters.numberOfDayToDownload))
                .isAfterOrEqualTo(now().minusDays(parameters.numberOfDayToDownload).minusMinutes(1))
    }

}
