package com.github.davinkevin.podcastserver.business.update

import arrow.core.Try
import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.business.CoverBusiness
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import com.github.davinkevin.podcastserver.manager.worker.*
import com.github.davinkevin.podcastserver.service.MessagingTemplate
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.utils.k
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.nio.file.Files
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import javax.annotation.PostConstruct
import javax.validation.Validator

@Component
class UpdatePodcastBusiness(
        val podcastRepository: PodcastRepository,
        val itemRepository: ItemRepository,
        val template: MessagingTemplate,
        val podcastServerParameters: PodcastServerParameters,
        val coverBusiness: CoverBusiness
) {

    val log = LoggerFactory.getLogger(this.javaClass.name)!!

    fun deleteOldCover() {
        log.info("Deletion of old covers item")

        itemRepository
                .findAllToDelete(podcastServerParameters.limitToKeepCoverOnDisk())
                .flatMap { coverBusiness.getCoverPathOf(it) }
                .forEach { Try { Files.deleteIfExists(it) } }
    }

    @PostConstruct
    fun resetItemWithIncorrectState() {
        log.info("Reset of Started and Paused")

        itemRepository.findByStatus(Status.STARTED, Status.PAUSED)
                .map { it.apply { status = Status.NOT_DOWNLOADED } }
                .forEach { itemRepository.save(it) }
    }
}
