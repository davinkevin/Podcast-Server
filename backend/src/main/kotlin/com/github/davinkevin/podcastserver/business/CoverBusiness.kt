package com.github.davinkevin.podcastserver.business

import arrow.core.Option
import arrow.core.Try
import arrow.core.getOrElse
import arrow.core.toOption
import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.mashape.unirest.http.HttpResponse
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.repository.CoverRepository
import lan.dk.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.Objects.isNull

/**
 * Created by kevin on 08/06/2014 for Podcast-Server
 */
@Component
@Transactional
class CoverBusiness(val coverRepository: CoverRepository, val parameters: PodcastServerParameters, val urlService: UrlService) {

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    fun findOne(id: UUID): Cover = coverRepository.findById(id).orElseThrow { Error("Cover with ID $id not found") }

    fun download(podcast: Podcast): String {
        val cover = podcast.cover

        if ( (cover?.url ?: "").isEmpty() ) {
            return ""
        }

        if (cover.url.startsWith("/")) {
            return cover.url
        }

        val coverUrl = cover.url
        val extension = FilenameUtils.getExtension(coverUrl)
        val fileLocation = parameters.rootfolder
                .resolve(podcast.title)
                .resolve("${parameters.coverDefaultName}.$extension")

        return urlToDisk(coverUrl, fileLocation)
                .map { String.format(Podcast.COVER_PROXY_URL, podcast.id.toString(), extension) }
                .getOrElse {
                    log.error("Error during downloading of the cover", it)
                    ""
                }
    }

    fun download(item: Item): Boolean {
        if (item.podcast == null || item.id == null) {
            log.error("Podcast or ID of item should not be null for element with title {}", item.title)
            return false
        }

        if ( (item.cover?.url ?: "").isEmpty()) {
            log.error("Cover null or empty for item of id {}", item.id)
            return false
        }

        val coverUrl = item.cover.url
        val fileLocation = parameters.rootfolder
                .resolve(item.podcast.title)
                .resolve("${item.id}.${FilenameUtils.getExtension(coverUrl)}")

        return urlToDisk(coverUrl, fileLocation)
                .map { true }
                .getOrElse{
                    log.error("Error during downloading of the cover", it)
                    false
                }
    }

    fun hasSameCoverURL(patchPodcast: Podcast, podcastToUpdate: Podcast): Boolean =
            !isNull(patchPodcast.cover)
                    && !isNull(podcastToUpdate.cover)
                    && patchPodcast.cover == podcastToUpdate.cover

    fun getCoverPathOf(podcast: Podcast): Path {
        val fileName = "${parameters.coverDefaultName}.${FilenameUtils.getExtension(podcast.cover.url)}"
        return parameters.rootfolder.resolve(podcast.title).resolve(fileName)
    }

    fun getCoverPathOf(i: Item): io.vavr.control.Option<Path> {
        return Option.fromNullable(i.cover)
                .map { it.url }
                .map { FilenameUtils.getExtension(it) }
                .map { "${i.id}.$it" }
                .map { parameters.rootfolder.resolve(i.podcast.title).resolve(it) }
                .toVΛVΓ()
    }

    fun save(cover: Cover): Cover = coverRepository.save(cover)

    private fun urlToDisk(coverUrl: String, fileLocation: Path) = Try {
        createParentDirectory(fileLocation)
        val r = imageRequest(coverUrl)
        Files.copy(r.body, fileLocation, StandardCopyOption.REPLACE_EXISTING)
        r
    }

    private fun imageRequest(coverUrl: String): HttpResponse<InputStream> {
        val v = urlService.get(coverUrl).asBinary()

        if (!isImage(v)) {
            throw RuntimeException("Not an image in content type")
        }

        return  v
    }

    private fun isImage(request: HttpResponse<InputStream>): Boolean {
        return request.headers["Content-Type"]
                ?.find { it.contains("image") }
                .toOption()
                .isDefined()
    }

    private fun createParentDirectory(fileLocation: Path) {
        if (!Files.exists(fileLocation.parent)) {
            Files.createDirectories(fileLocation.parent)
        }
    }

}
