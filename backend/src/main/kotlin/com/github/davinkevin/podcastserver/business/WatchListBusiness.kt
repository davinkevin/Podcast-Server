package com.github.davinkevin.podcastserver.business

import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.utils.k
import lan.dk.podcastserver.repository.WatchListRepository
import org.springframework.stereotype.Component
import java.util.*

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Component
class WatchListBusiness(val watchListRepository: WatchListRepository, val jdomService: JdomService) {

    fun delete(uuid: UUID) = watchListRepository.deleteById(uuid)

    fun asRss(id: UUID, domainFromRequest: String) = watchListRepository.findById(id).k()
            .map { jdomService.watchListToXml(it, domainFromRequest) }
            .getOrElse { throw RuntimeException("Rss generation of watchlist $id caused Error") }
}

