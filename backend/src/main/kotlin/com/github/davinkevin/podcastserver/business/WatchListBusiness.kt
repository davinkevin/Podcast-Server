package com.github.davinkevin.podcastserver.business

import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.entity.WatchList
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.utils.k
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.WatchListRepository
import org.springframework.stereotype.Component
import java.util.*

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Component
class WatchListBusiness(val watchListRepository: WatchListRepository, val itemRepository: ItemRepository, val jdomService: JdomService) {

    fun add(watchListId: UUID, itemId: UUID): WatchList {
        val watchList = watchListRepository.findById(watchListId).k()
                .getOrElse { throw RuntimeException("Watchlist not found") }
        val item = itemRepository.findById(itemId).k()
                .getOrElse { throw RuntimeException("Item with ID $itemId not found") }

        return watchListRepository.save(watchList.apply { add(item) })
    }

    fun remove(watchListId: UUID, itemId: UUID): WatchList {
        val watchList = watchListRepository.findById(watchListId).k()
                .getOrElse { throw RuntimeException("Watchlist not found") }
        val item = itemRepository.findById(itemId).k()
                .getOrElse { throw RuntimeException("Item with ID $itemId not found") }

        return watchListRepository.save(watchList.apply { remove(item) })
    }

    fun delete(uuid: UUID) = watchListRepository.deleteById(uuid)

    fun asRss(id: UUID, domainFromRequest: String) = watchListRepository.findById(id).k()
            .map { jdomService.watchListToXml(it, domainFromRequest) }
            .getOrElse { throw RuntimeException("Rss generation of watchlist $id caused Error") }
}

