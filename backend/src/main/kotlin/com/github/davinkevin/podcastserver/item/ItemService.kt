package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.toMono
import java.util.*
import com.github.davinkevin.podcastserver.item.ItemRepositoryV2 as ItemRepository

/**
 * Created by kevin on 2019-02-09
 */
@Component
class ItemService(
        private val repository: ItemRepository,
        private val p: PodcastServerParameters,
        private val fileService: FileService
) {

    private val log = LoggerFactory.getLogger(ItemService::class.java)!!

    fun deleteOldEpisodes() = repository.
            findAllToDelete( p.limitDownloadDate().toOffsetDateTime() )
            .doOnSubscribe { log.info("Deletion of old items") }
            .delayUntil { fileService.deleteItem(it.path) }
            .collectList()
            .flatMap { repository.deleteById(it.map { v -> v.id }) }

    fun findById(id: UUID) = repository.findById(id)

}
