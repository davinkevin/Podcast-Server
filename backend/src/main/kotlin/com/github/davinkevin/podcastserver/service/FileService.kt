package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.item.CoverForItem
import com.github.davinkevin.podcastserver.item.DeleteItemInformation
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.podcast.CoverForPodcast
import com.github.davinkevin.podcastserver.podcast.Podcast
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by kevin on 2019-02-09
 */
@Service
class FileService(
        private val p: PodcastServerParameters,
        private val mimeTypeService: MimeTypeService,
        private val wcb: WebClient.Builder
) {

    private val log = LoggerFactory.getLogger(FileService::class.java)

    fun deleteItem(item: DeleteItemInformation) = Mono.defer {
        val file = p.rootfolder.resolve(item.path)
        log.info("Deletion of file {}", file)
        Files
                .deleteIfExists(file)
                .toMono()
    }

    fun coverExists(p: Podcast): Mono<String> = exists(
            Paths.get(p.title).resolve("${p.id}.${p.cover.extension()}")
    )

    fun coverExists(i: Item): Mono<String> = exists(
            Paths.get(i.podcast.title).resolve("${i.id}.${i.cover.extension()}")
    )

    private fun exists(path: Path) = Mono.defer {
        val file = p.rootfolder.resolve(path)
        val exists = Files.exists(file)
        log.info("the file $file exists: $exists")

        exists
                .toMono()
                .filter { it }
                .map { path }
                .map { it.toString().substringAfterLast("/") }
    }

    fun downloadPodcastCover(podcast: Podcast): Mono<Void> {
        return wcb.clone()
                .baseUrl(podcast.cover.url.toASCIIString())
                .build()
                .get()
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(ByteArrayResource::class.java)
                .map { val file = p.rootfolder
                            .resolve(podcast.title)
                            .create()
                            .resolve("${podcast.id}.${podcast.cover.extension()}")

                    log.debug("Save file ${file.toAbsolutePath()}")
                    Files.write(file, it.byteArray) }
                .then()
    }

    fun movePodcast(oldPodcast: Podcast, newTitle: String): Mono<Void> = Mono.defer {
        val oldLocation = p.rootfolder.resolve(oldPodcast.title)
        val newLocation = p.rootfolder.resolve(newTitle)

        log.info("Move podcast from $oldLocation to $newLocation")

        Files.move(oldLocation, newLocation)
                .toMono()
                .then()
    }

    fun upload(destination: Path, file: FilePart): Mono<Void> {
        Files.deleteIfExists(destination)
        Files.createDirectories(destination.parent)
        return file.transferTo(destination)
    }

    fun size(file: Path): Mono<Long> = Files.size(file).toMono()

    fun probeContentType(file: Path): Mono<String> = mimeTypeService.probeContentType(file).toMono()
}

private fun Path.create() = if (Files.exists(this)) this else Files.createDirectory(this)
private fun CoverForPodcast.extension() = FilenameUtils.getExtension(url.toASCIIString()) ?: "jpg"
private fun CoverForItem.extension() = FilenameUtils.getExtension(url) ?: "jpg"
