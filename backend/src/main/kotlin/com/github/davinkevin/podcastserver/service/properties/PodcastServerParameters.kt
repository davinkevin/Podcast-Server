package com.github.davinkevin.podcastserver.service.properties

import com.github.davinkevin.podcastserver.database.Tables.APPLICATION_SETTINGS
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL
import java.time.OffsetDateTime
import java.time.ZonedDateTime

class PodcastServerParameters(private val dsl: DSLContext) {

    val concurrentDownload: Int
        get() = latest(APPLICATION_SETTINGS.CONCURRENT_DOWNLOAD)

    val numberOfTry: Int
        get() = latest(APPLICATION_SETTINGS.NUMBER_OF_TRY)

    val numberOfDayToDownload: Long
        get() = latest(APPLICATION_SETTINGS.NUMBER_OF_DAY_TO_DOWNLOAD)

    val numberOfDayToSaveCover: Long
        get() = latest(APPLICATION_SETTINGS.NUMBER_OF_DAY_TO_SAVE_COVER)

    fun limitDownloadDate(): OffsetDateTime = OffsetDateTime.now().minusDays(numberOfDayToDownload)
    fun limitToKeepCoverOnDisk(): ZonedDateTime = ZonedDateTime.now().minusDays(numberOfDayToSaveCover)

    fun updateConcurrentDownload(value: Int) = appendOverriding(APPLICATION_SETTINGS.CONCURRENT_DOWNLOAD, value)
    fun updateNumberOfTry(value: Int) = appendOverriding(APPLICATION_SETTINGS.NUMBER_OF_TRY, value)
    fun updateNumberOfDayToDownload(value: Long) = appendOverriding(APPLICATION_SETTINGS.NUMBER_OF_DAY_TO_DOWNLOAD, value)
    fun updateNumberOfDayToSaveCover(value: Long) = appendOverriding(APPLICATION_SETTINGS.NUMBER_OF_DAY_TO_SAVE_COVER, value)

    private fun <T> latest(field: Field<T>): T = dsl
        .select(field)
        .from(APPLICATION_SETTINGS)
        .orderBy(APPLICATION_SETTINGS.ID.desc())
        .limit(1)
        .fetchSingle()
        .get(field)!!

    private fun <T> appendOverriding(target: Field<T>, value: T) {
        val columns = listOf(
            APPLICATION_SETTINGS.CONCURRENT_DOWNLOAD,
            APPLICATION_SETTINGS.NUMBER_OF_TRY,
            APPLICATION_SETTINGS.NUMBER_OF_DAY_TO_DOWNLOAD,
            APPLICATION_SETTINGS.NUMBER_OF_DAY_TO_SAVE_COVER,
        )
        val selected = columns.map { if (it === target) DSL.value(value, target.dataType) else it }

        dsl.insertInto(APPLICATION_SETTINGS)
            .columns(columns)
            .select(
                dsl.select(selected)
                    .from(APPLICATION_SETTINGS)
                    .orderBy(APPLICATION_SETTINGS.ID.desc())
                    .limit(1)
            )
            .execute()
    }
}
