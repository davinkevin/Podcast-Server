package com.github.davinkevin.podcastserver.business.stats

import com.github.davinkevin.podcastserver.business.stats.StatsBusiness.Selector.*
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.querydsl.core.types.dsl.BooleanExpression
import io.vavr.collection.List
import io.vavr.collection.Set
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.dsl.ItemDSL.*
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
@Component
class StatsBusiness(val itemRepository: ItemRepository, val updaterSelector: UpdaterSelector) {

    fun allStatsByTypeAndDownloadDate(numberOfMonth: Int) =
            allStatsByType(numberOfMonth, BY_DOWNLOAD_DATE)

    fun allStatsByTypeAndCreationDate(numberOfMonth: Int) =
            allStatsByType(numberOfMonth, BY_CREATION_DATE)

    fun allStatsByTypeAndPubDate(numberOfMonth: Int) =
            allStatsByType(numberOfMonth, BY_PUBLICATION_DATE)

    fun statsByPubDate(id: UUID, numberOfMonth: Long): Set<NumberOfItemByDateWrapper> =
            itemRepository.findStatOfPubDate(id, numberOfMonth.toInt())

    fun statsByDownloadDate(id: UUID, numberOfMonth: Long): Set<NumberOfItemByDateWrapper> =
            itemRepository.findStatOfDownloadDate(id, numberOfMonth.toInt())

    fun statsByCreationDate(id: UUID, numberOfMonth: Long): Set<NumberOfItemByDateWrapper> =
            itemRepository.findStatOfCreationDate(id, numberOfMonth.toInt())

    private fun generateForType(type: Type, numberOfMonth: Int, selector: Selector): StatsPodcastType {
        val dateInPast = ZonedDateTime.now().minusMonths(numberOfMonth.toLong())

        val values = itemRepository
                .findByTypeAndExpression(type, selector.filter(dateInPast))
                .toJavaList()
                .map(selector.date)
                .filter { Objects.nonNull(it) }
                .map { it!!.toLocalDate() }
                .toNumberOfItem()

        return StatsPodcastType(type.name, values)
    }

    private fun allStatsByType(numberOfMonth: Int, selector: Selector): List<StatsPodcastType> {
        return updaterSelector
                .types()
                .toJavaList()
                .map { generateForType(it, numberOfMonth, selector) }
                .filter { stats -> !stats.isEmpty }
                .sortedBy { it.type }
                .toVΛVΓ()
    }

    private enum class Selector(val date: (Item) -> ZonedDateTime?, val filter: (ZonedDateTime) ->  BooleanExpression) {
        BY_DOWNLOAD_DATE({ it.downloadDate }, ::hasBeenDownloadedAfter),
        BY_CREATION_DATE({ it.creationDate }, ::hasBeenCreatedAfter),
        BY_PUBLICATION_DATE({ it.pubDate }, ::isNewerThan)
    }

    fun kotlin.collections.List<LocalDate>.toNumberOfItem() = this
            .groupBy { it }
            .mapValues { it.value.size }
            .map { NumberOfItemByDateWrapper(it.key, it.value) }
            .toSet()
            .toVΛVΓ()
}
