package com.github.davinkevin.podcastserver.service.properties

import com.github.davinkevin.podcastserver.database.Tables.APPLICATION_SETTINGS
import com.github.davinkevin.podcastserver.extension.spring.NestedSpringTest
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jooq.test.autoconfigure.JooqTest
import org.springframework.context.annotation.Import
import java.time.OffsetDateTime
import java.time.ZonedDateTime

@JooqTest
@Import(PodcastServerParameters::class)
@NestedSpringTest
class PodcastServerParametersTest(
    @Autowired private val parameters: PodcastServerParameters,
    @Autowired private val dsl: DSLContext,
) {

    @BeforeEach
    fun resetToSeedRow() {
        // Migration V11 seeds one row with the defaults; wipe additional rows
        // accumulated by previous tests and reinsert the seed if needed.
        dsl.deleteFrom(APPLICATION_SETTINGS).execute()
        dsl.insertInto(APPLICATION_SETTINGS).defaultValues().execute()
    }

    @Test
    fun `reads the default values from the seed row`() {
        assertThat(parameters.concurrentDownload).isEqualTo(3)
        assertThat(parameters.numberOfTry).isEqualTo(10)
        assertThat(parameters.numberOfDayToDownload).isEqualTo(30L)
        assertThat(parameters.numberOfDayToSaveCover).isEqualTo(365L)
    }

    @Test
    fun `limitDownloadDate is now minus numberOfDayToDownload`() {
        assertThat(parameters.limitDownloadDate())
            .isBeforeOrEqualTo(OffsetDateTime.now().minusDays(30))
            .isAfterOrEqualTo(OffsetDateTime.now().minusDays(30).minusMinutes(1))
    }

    @Test
    fun `limitToKeepCoverOnDisk is now minus numberOfDayToSaveCover`() {
        assertThat(parameters.limitToKeepCoverOnDisk())
            .isBeforeOrEqualTo(ZonedDateTime.now().minusDays(365))
            .isAfterOrEqualTo(ZonedDateTime.now().minusDays(365).minusMinutes(1))
    }

    @Test
    fun `updateConcurrentDownload appends a new row and reads see it`() {
        parameters.updateConcurrentDownload(8)

        assertThat(parameters.concurrentDownload).isEqualTo(8)
        assertThat(rowCount()).isEqualTo(2)
    }

    @Test
    fun `updateNumberOfTry appends a new row and reads see it`() {
        parameters.updateNumberOfTry(42)

        assertThat(parameters.numberOfTry).isEqualTo(42)
        assertThat(rowCount()).isEqualTo(2)
    }

    @Test
    fun `updateNumberOfDayToDownload appends a new row and reads see it`() {
        parameters.updateNumberOfDayToDownload(7L)

        assertThat(parameters.numberOfDayToDownload).isEqualTo(7L)
        assertThat(rowCount()).isEqualTo(2)
    }

    @Test
    fun `updateNumberOfDayToSaveCover appends a new row and reads see it`() {
        parameters.updateNumberOfDayToSaveCover(90L)

        assertThat(parameters.numberOfDayToSaveCover).isEqualTo(90L)
        assertThat(rowCount()).isEqualTo(2)
    }

    @Test
    fun `update preserves unrelated fields by copying them into the new row`() {
        parameters.updateConcurrentDownload(8)
        parameters.updateNumberOfTry(42)

        assertThat(parameters.concurrentDownload).isEqualTo(8)
        assertThat(parameters.numberOfTry).isEqualTo(42)
        assertThat(parameters.numberOfDayToDownload).isEqualTo(30L)
        assertThat(parameters.numberOfDayToSaveCover).isEqualTo(365L)
    }

    private fun rowCount(): Int = dsl.fetchCount(APPLICATION_SETTINGS)
}
