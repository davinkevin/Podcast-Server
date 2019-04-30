package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.cover.CoverRepositoryV2
import com.github.davinkevin.podcastserver.cover.DownloadPodcastCoverInformation
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.tag.Tag
import com.github.davinkevin.podcastserver.tag.TagRepositoryV2
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.util.function.component1
import reactor.util.function.component2
import java.net.URI
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
                        url = p.url.toASCIIString(),
                        hasToBeDeleted = p.hasToBeDeleted,
                        type = p.type,
                        tags = t,
                        cover = c)
                }
                .delayUntil { fileService.downloadPodcastCover(it) }
    }
}


data class PodcastForCreation(val title: String, val url: URI, val tags: Collection<TagForCreation>, val type: String, val hasToBeDeleted: Boolean, val cover: CoverForCreation)
data class TagForCreation(val id: UUID?, val name: String)

