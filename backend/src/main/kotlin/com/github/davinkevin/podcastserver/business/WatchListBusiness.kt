package com.github.davinkevin.podcastserver.business

import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.API.Set
import lan.dk.podcastserver.entity.WatchList
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.WatchListRepository
import org.springframework.stereotype.Component
import java.util.*

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Component
class WatchListBusiness(val watchListRepository: WatchListRepository, val itemRepository: ItemRepository, val jdomService: JdomService) {

    fun findOne(id: UUID): WatchList =
            watchListRepository.findById(id).k()
                    .getOrElse { throw RuntimeException("Watchlist not found") }

    fun findAll() =
            watchListRepository
                    .findAll()
                    .toSet()
                    .toVΛVΓ()

    fun findContainsItem(itemId: UUID) =
            itemRepository.findById(itemId).k()
                    .map { watchListRepository.findContainsItem(it) }
                    .getOrElse { Set() }


    fun add(watchListId: UUID, itemId: UUID): WatchList {
        val watchList = findOne(watchListId)
        val item = itemRepository.findById(itemId).k()
                .getOrElse { throw RuntimeException("Item with ID $itemId not found") }

        return watchListRepository.save(watchList.add(item))
    }

    fun remove(watchListId: UUID, itemId: UUID): WatchList {
        val watchList = findOne(watchListId)
        val item = itemRepository.findById(itemId).k()
                .getOrElse { throw RuntimeException("Item with ID $itemId not found") }

        return watchListRepository.save(watchList.remove(item))
    }

    fun delete(uuid: UUID) = watchListRepository.deleteById(uuid)

    fun save(watchList: WatchList) = watchListRepository.save(watchList)

    fun asRss(id: UUID, domainFromRequest: String) = watchListRepository.findById(id).k()
            .map { jdomService.watchListToXml(it, domainFromRequest) }
            .getOrElse { throw RuntimeException("Rss generation of watchlist $id caused Error") }
}

