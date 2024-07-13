package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.cover.CoverRepository
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import com.github.davinkevin.podcastserver.service.storage.MovePodcastRequest
import com.github.davinkevin.podcastserver.tag.Tag
import com.github.davinkevin.podcastserver.tag.TagRepository
import java.util.*

class PodcastService(
    private val repository: PodcastRepository,
    private val coverRepository: CoverRepository,
    private val tagRepository: TagRepository,
    private val fileService: FileStorageService
) {

    fun findAll(): List<Podcast> = repository.findAll()
    fun findById(id: UUID): Podcast? = repository.findById(id)

    fun findStatByPodcastIdAndPubDate(id: UUID, numberOfMonths: Int): List<NumberOfItemByDateWrapper> =
        repository.findStatByPodcastIdAndPubDate(id, numberOfMonths)
    fun findStatByPodcastIdAndDownloadDate(id: UUID, numberOfMonths: Int): List<NumberOfItemByDateWrapper> =
        repository.findStatByPodcastIdAndDownloadDate(id, numberOfMonths)
    fun findStatByPodcastIdAndCreationDate(id: UUID, numberOfMonths: Int): List<NumberOfItemByDateWrapper> =
        repository.findStatByPodcastIdAndCreationDate(id, numberOfMonths)

    fun findStatByTypeAndCreationDate(numberOfMonths: Int): List<StatsPodcastType> =
        repository.findStatByTypeAndCreationDate(numberOfMonths)
    fun findStatByTypeAndPubDate(numberOfMonths: Int): List<StatsPodcastType> =
        repository.findStatByTypeAndPubDate(numberOfMonths)
    fun findStatByTypeAndDownloadDate(numberOfMonths: Int): List<StatsPodcastType> =
        repository.findStatByTypeAndDownloadDate(numberOfMonths)

    fun save(p: PodcastForCreation): Podcast {
        val oldTags = p.tags.filter { it.id != null }.map { Tag(it.id!!, it.name) }
        val newTags = p.tags.filter { it.id == null }.map { tagRepository.save(it.name) }

        val tags = oldTags + newTags
        val cover = coverRepository.save(p.cover).block()!!

        val podcast = repository.save(
            title = p.title,
            url = p.url?.toASCIIString(),
            hasToBeDeleted = p.hasToBeDeleted,
            type = p.type,
            tags = tags,
            cover = cover
        )

        fileService.downloadPodcastCover(podcast).block()

        return podcast
    }

    fun update(updatePodcast: PodcastForUpdate): Podcast {
        val p = findById(updatePodcast.id)
            ?: error("Trying to upgrade a non existent podcast")

        val oldTags = updatePodcast.tags.filter { it.id != null }.map { Tag(it.id!!, it.name) }
        val newTags = updatePodcast.tags.filter { it.id == null }.map { tagRepository.save(it.name) }
        val tags = oldTags + newTags

        val newCover = updatePodcast.cover
        val oldCover = p.cover

        val isLocalCover = newCover.url.toASCIIString().startsWith("/")
        val coversAreIdentical = oldCover.url != newCover.url
        val cover = if (!isLocalCover && coversAreIdentical)
            coverRepository.save(newCover).block()!!.also {
                fileService
                    .downloadPodcastCover(p.copy(cover = Cover(it.id, it.url, it.height, it.width)))
                    .block()
            }
        else Cover(oldCover.id, oldCover.url, oldCover.height, oldCover.width)

        val podcast = repository.update(
            id = updatePodcast.id,
            title = updatePodcast.title,
            url = updatePodcast.url?.toASCIIString(),
            hasToBeDeleted = updatePodcast.hasToBeDeleted,
            tags = tags,
            cover = cover
        )

        val podcastTitleHasChanged = p.title != updatePodcast.title
        if (podcastTitleHasChanged) {
            val movePodcastDetails = MovePodcastRequest(
                id = updatePodcast.id,
                from = p.title,
                to = updatePodcast.title
            )
            fileService.movePodcast(movePodcastDetails).block()
        }

        return podcast
    }

    fun deleteById(id: UUID) {
        val request = repository.deleteById(id) ?: return

        fileService.deletePodcast(request).block()
    }
}

