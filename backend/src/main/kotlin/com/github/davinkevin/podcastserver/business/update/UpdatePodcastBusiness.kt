package com.github.davinkevin.podcastserver.business.update

import arrow.core.Try
import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.business.CoverBusiness
import com.github.davinkevin.podcastserver.utils.k
import io.vavr.Tuple3
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import lan.dk.podcastserver.manager.worker.Updater
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import lan.dk.podcastserver.service.properties.PodcastServerParameters
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate
import java.util.function.Supplier
import javax.annotation.PostConstruct
import javax.validation.Validator

@Component
class UpdatePodcastBusiness(val podcastRepository: PodcastRepository, val itemRepository: ItemRepository, val updaterSelector: UpdaterSelector, val template: SimpMessagingTemplate, val podcastServerParameters: PodcastServerParameters, @param:Qualifier("UpdateExecutor") val updateExecutor: ThreadPoolTaskExecutor, @param:Qualifier("ManualUpdater") val manualExecutor: ThreadPoolTaskExecutor, @param:Qualifier("Validator") val validator: Validator, val coverBusiness: CoverBusiness) {

    val log = LoggerFactory.getLogger(this.javaClass.name)!!

    private var timeUnit = TimeUnit.MINUTES
    private var timeValue: Int = 5

    internal fun setTimeOut(timeValue: Int, timeUnit: TimeUnit) {
        this.timeValue = timeValue
        this.timeUnit = timeUnit
    }

    var lastFullUpdate: ZonedDateTime? = null

    private val _isUpdating = AtomicBoolean(false)
    val isUpdating: Boolean
        get() = _isUpdating.get()

    val updaterActiveCount: Int
        get() = updateExecutor.activeCount + manualExecutor.activeCount

    @Transactional
    fun updatePodcast() {
        updatePodcast(podcastRepository.findByUrlIsNotNull().toJavaSet(), updateExecutor)
        lastFullUpdate = ZonedDateTime.now()
    }

    @Transactional
    fun updatePodcast(id: UUID) {
        val podcast = podcastRepository.findById(id)
                .k()
                .getOrElse{ throw RuntimeException("Podcast with ID $id not found") }

        updatePodcast(setOf(podcast), manualExecutor)
    }

    @Transactional
    fun forceUpdatePodcast(id: UUID) {
        log.info("Launch forced update on podcast with id: $id")
        var podcast = podcastRepository.findById(id)
                .k()
                .getOrElse{ throw RuntimeException("Podcast with ID $id not found") }
        podcast.signature = ""
        podcast = podcastRepository.save(podcast)
        updatePodcast(podcast.id)
    }

    private fun updatePodcast(podcasts: kotlin.collections.Set<Podcast>, selectedExecutor: Executor) {
        changeAndCommunicateUpdate(true)

        log.info("Update launch")
        log.info("About to update {} podcast(s)", podcasts.size)

        podcasts
                .map { supplyAsync(Supplier { updaterSelector.of(it.url).update(it) }, selectedExecutor) }
                .stream()
                .map { this.wait(it) }
                .map { UpdatePodcastInformation(it) }
                .filter { it != NO_MODIFICATION }
                .peek { changeAndCommunicateUpdate(true) }
                .map { attachNewItemsToPodcast(it.podcast, it.items, it.p) }
                .flatMap { it.stream() }
                .forEach { coverBusiness.download(it) }

        log.info("End of treatment on {} podcasts", podcasts.size)

        changeAndCommunicateUpdate(false)
    }

    private fun changeAndCommunicateUpdate(isUpdating: Boolean) {
        _isUpdating.set(isUpdating)
        this.template.convertAndSend(WS_TOPIC_UPDATING, _isUpdating.get())
    }

    private fun wait(future: CompletableFuture<Tuple3<Podcast, io.vavr.collection.Set<Item>, Predicate<Item>>>): Tuple3<Podcast, io.vavr.collection.Set<Item>, Predicate<Item>> =
            Try { future.get(timeValue.toLong(), timeUnit) }
                    .getOrElse {
                        log.error("Error during update", it)
                        future.cancel(true)
                        Updater.NO_MODIFICATION_TUPLE
                    }

    private fun attachNewItemsToPodcast(podcast: Podcast, items: kotlin.collections.Set<Item>, filter: (Item) -> Boolean): kotlin.collections.Set<Item> {

        if (items.isEmpty()) {
            log.info("Reset of signature in order to force the next update: {}", podcast.title)
            podcastRepository.save(podcast.setSignature(""))
            return setOf()
        }

        val itemsToAdd = items
                .filter(filter)
                .map { item -> item.setPodcast(podcast) }
                .filter { item -> validator.validate(item).isEmpty() }

        if (itemsToAdd.isEmpty()) {
            return itemsToAdd
                    .toSet()
        }

        itemRepository.saveAll(
                itemsToAdd
                        .map { podcast.add(it); it.setStatus(Status.NOT_DOWNLOADED) }
                        .map { it.setNumberOfFail(0) }
        )

        podcastRepository.save(podcast.lastUpdateToNow())

        return itemsToAdd.toSet()
    }

    fun deleteOldEpisode() {
        log.info("Deletion of olds items")

        itemRepository
                .findAllToDelete(podcastServerParameters.limitDownloadDate())
                .toStream()
                .peek { log.info("Deletion of file associated with item {} : {}", it.id, it.localPath.toAbsolutePath()) }
                .map { it.deleteDownloadedFile() }
                .forEach { itemRepository.save(it) }
    }

    fun deleteOldCover() {
        log.info("Deletion of old covers item")

        itemRepository
                .findAllToDelete(podcastServerParameters.limitToKeepCoverOnDisk())
                .flatMap { coverBusiness.getCoverPathOf(it) }
                .forEach { arrow.core.Try { Files.deleteIfExists(it) } }
    }

    @PostConstruct
    fun resetItemWithIncorrectState() {
        log.info("Reset of Started and Paused")

        itemRepository.findByStatus(Status.STARTED, Status.PAUSED)
                .map { it.setStatus(Status.NOT_DOWNLOADED) }
                .forEach { itemRepository.save(it) }
    }

    companion object {
        private const val WS_TOPIC_UPDATING = "/topic/updating"
        val NO_MODIFICATION = UpdatePodcastInformation(Updater.NO_MODIFICATION_TUPLE)
    }
}

data class UpdatePodcastInformation(val podcast: Podcast, val items: kotlin.collections.Set<Item>, val p: (Item) -> Boolean) {
    constructor(t: Tuple3<Podcast, io.vavr.collection.Set<Item>, Predicate<Item>>)
            : this(t._1, t._2.toJavaSet(), t._3::test)
}