package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.cover.CoverRepositoryV2
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.tag.Tag
import com.github.davinkevin.podcastserver.tag.TagRepositoryV2
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import reactor.util.function.component1
import reactor.util.function.component2
import java.util.*
import com.github.davinkevin.podcastserver.podcast.PodcastRepositoryV2 as PodcastRepository

@Service
class PodcastService(
        private val repository: PodcastRepository,
        private val coverRepository: CoverRepositoryV2,
        private val tagRepository: TagRepositoryV2,
        private val fileService: FileService
) {

    fun findAll(): Flux<Podcast> = repository.findAll()
    fun findById(id: UUID): Mono<Podcast> = repository.findById(id)

    fun findStatByPodcastIdAndPubDate(id: UUID, numberOfMonths: Int) = repository.findStatByPodcastIdAndPubDate(id, numberOfMonths)
    fun findStatByPodcastIdAndDownloadDate(id: UUID, numberOfMonths: Int) = repository.findStatByPodcastIdAndDownloadDate(id, numberOfMonths)
    fun findStatByPodcastIdAndCreationDate(id: UUID, numberOfMonths: Int) = repository.findStatByPodcastIdAndCreationDate(id, numberOfMonths)

    fun findStatByTypeAndCreationDate(numberOfMonths: Int) = repository.findStatByTypeAndCreationDate(numberOfMonths)
    fun findStatByTypeAndPubDate(numberOfMonths: Int) = repository.findStatByTypeAndPubDate(numberOfMonths)
    fun findStatByTypeAndDownloadDate(numberOfMonths: Int) = repository.findStatByTypeAndDownloadDate(numberOfMonths)

    fun save(p: PodcastForCreation): Mono<Podcast> {

        val oldTags = p.tags.toFlux().filter { it.id != null }.map { Tag(it.id!!, it.name) }
        val newTags = p.tags.toFlux().filter { it.id == null }.flatMap { tagRepository.save(it.name) }

        val tags = Flux.merge(oldTags, newTags).collectList()
        val cover = coverRepository.save(p.cover)

        return Mono.zip(tags, cover)
                .flatMap { (t, c) ->  repository.save(
                        title = p.title,
                        url = p.url?.toASCIIString(),
                        hasToBeDeleted = p.hasToBeDeleted,
                        type = p.type,
                        tags = t,
                        cover = c)
                }
                .delayUntil { fileService.downloadPodcastCover(it) }
    }

    fun update(updatePodcast: PodcastForUpdate): Mono<Podcast> = findById(updatePodcast.id).flatMap { p ->

        val oldTags = updatePodcast.tags.toFlux().filter { it.id != null }.map { Tag(it.id!!, it.name) }
        val newTags = updatePodcast.tags.toFlux().filter { it.id == null }.flatMap { tagRepository.save(it.name) }
        val tags = Flux.merge(oldTags, newTags).collectList()

        val newCover = updatePodcast.cover
        val oldCover = p.cover
        val cover =
                if (!newCover.url.toASCIIString().startsWith("/") && oldCover.url != newCover.url)
                    coverRepository.save(newCover).delayUntil {
                        fileService.downloadPodcastCover(p.copy(cover = CoverForPodcast(it.id, it.url, it.height, it.width)))
                    }
                else Cover(oldCover.id, oldCover.url, oldCover.height, oldCover.width).toMono()

        val title =
                if (p.title != updatePodcast.title) fileService.movePodcast(p, updatePodcast.title)
                else Mono.empty()

        Mono.zip(tags, cover)
                .flatMap { (t, c) ->
                    repository.update(
                            id = updatePodcast.id,
                            title = updatePodcast.title,
                            url = updatePodcast.url?.toASCIIString(),
                            hasToBeDeleted = updatePodcast.hasToBeDeleted,
                            tags = t,
                            cover = c)
                }
                .delayUntil { title }
    }

    fun deleteById(id: UUID): Mono<Void> =
            repository
                    .deleteById(id)
                    .delayUntil { fileService.deletePodcast(it) }
                    .then()
}

