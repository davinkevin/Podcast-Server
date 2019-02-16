package com.github.davinkevin.podcastserver.podcast

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*
import com.github.davinkevin.podcastserver.podcast.PodcastRepositoryV2 as PodcastRepository

@Service
class PodcastService(private val repository: PodcastRepository) {

    fun findById(id: UUID): Mono<Podcast> = repository.findById(id)
    fun findStatByPubDate(id: UUID, numberOfMonths: Int) = repository.findStatByPubDate(id, numberOfMonths)
    fun findStatByDownloadDate(id: UUID, numberOfMonths: Int) = repository.findStatByDownloadDate(id, numberOfMonths)
    fun findStatByCreationDate(id: UUID, numberOfMonths: Int) = repository.findStatByCreationDate(id, numberOfMonths)

}
