package com.github.davinkevin.podcastserver.business

import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.utils.k
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.repository.PodcastRepository
import lan.dk.podcastserver.service.properties.PodcastServerParameters
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Component
@Transactional
class PodcastBusiness(val parameters: PodcastServerParameters, val jdomService: JdomService, val podcastRepository: PodcastRepository, val tagBusiness: TagBusiness, val coverBusiness: CoverBusiness) {

    fun findAll(): List<Podcast> = podcastRepository.findAll()

    fun save(entity: Podcast): Podcast = podcastRepository.save(entity)

    fun findOne(id: UUID): Podcast =
            podcastRepository.findById(id).k()
                    .getOrElse { throw RuntimeException("Podcast $id not found") }

    //TODO : Delete the folder with java.nio.PATH and java.nio.FILES
    fun delete(id: UUID) = podcastRepository.deleteById(id)

    //TODO : Delete the folder with java.nio.PATH and java.nio.FILES
    fun delete(entity: Podcast) = podcastRepository.delete(entity)

    fun patchUpdate(patchPodcast: Podcast): Podcast {
        val podcastToUpdate = findOne(patchPodcast.id)

        if (podcastToUpdate.title != patchPodcast.title) {
            Files.move(
                    parameters.rootfolder.resolve(podcastToUpdate.title),
                    parameters.rootfolder.resolve(patchPodcast.title)
            )
        }

        if (!coverBusiness.hasSameCoverURL(patchPodcast, podcastToUpdate)) {
            patchPodcast.cover.url = coverBusiness.download(patchPodcast)
        }

        val newCover = coverBusiness.findOne(patchPodcast.cover.id).apply {
            height = patchPodcast.cover.height
            url = patchPodcast.cover.url
            width = patchPodcast.cover.width
        }

        podcastToUpdate.apply {
                title = patchPodcast.title
                url = patchPodcast.url
                signature = patchPodcast.signature
                type = patchPodcast.type
                description = patchPodcast.description
                hasToBeDeleted = patchPodcast.hasToBeDeleted
                tags = tagBusiness.getTagListByName(patchPodcast.tags).toMutableSet()
                cover = newCover
        }

        return save(podcastToUpdate)
    }

    @Transactional(readOnly = true)
    fun getRss(id: UUID, limit: Boolean?, domainName: String): String =
            jdomService.podcastToXMLGeneric(findOne(id), domainName, limit)

    fun reatachAndSave(podcast: Podcast): Podcast {
        podcast.tags = tagBusiness.getTagListByName(podcast.tags)
        return save(podcast)
    }

    fun create(podcast: Podcast): Podcast {
        val podcastSaved = reatachAndSave(podcast)

        if (podcast.cover != null) {
            val cover = podcast.cover
            cover.url = coverBusiness.download(podcast)
            coverBusiness.save(cover)
        }

        return podcastSaved
    }

    fun coverOf(id: UUID): Path = coverBusiness.getCoverPathOf(findOne(id))

    fun asOpml(domainName: String): String = jdomService.podcastsToOpml(findAll(), domainName)
}
