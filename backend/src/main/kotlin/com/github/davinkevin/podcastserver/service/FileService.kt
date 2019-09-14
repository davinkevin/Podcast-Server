package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation
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
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import reactor.netty.http.client.HttpClient
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * Created by kevin on 2019-02-09
 */
@Service
class FileService(
        private val p: PodcastServerParameters,
        private val mimeTypeService: MimeTypeService,
        wcbs: WebClient.Builder
) {

    private val log = LoggerFactory.getLogger(FileService::class.java)

    private val wcb = wcbs
            .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))

    fun deleteItem(item: DeleteItemInformation) = Mono.defer {
        val file = p.rootfolder.resolve(item.path)
        log.info("Deletion of file {}", file)
        Files
                .deleteIfExists(file)
                .toMono()
    }

    fun deleteCover(cover: DeleteCoverInformation): Mono<Boolean> {
        val file = p.rootfolder
                .resolve(cover.podcast.title)
                .resolve(cover.item.id.toString() + "." + cover.extension)

        log.info("Deletion of file {}", file)

        return Files.deleteIfExists(file)
                .toMono()
    }

    fun coverExists(p: Podcast): Mono<String> = coverExists(p.title, p.id, p.cover.extension())
    fun coverExists(i: Item): Mono<String> = coverExists(i.podcast.title, i.id, i.cover.extension())
    fun coverExists(podcastTitle: String, itemId: UUID, extension: String): Mono<String> =
            exists(Paths.get(podcastTitle).resolve("$itemId.$extension"))

    private fun exists(path: Path) = Mono.defer {
        val file = p.rootfolder.resolve(path)
        val exists = Files.exists(file)
        log.debug("the file $file exists: $exists")

        exists
                .toMono()
                .filter { it }
                .map { path }
                .map { it.toString().substringAfterLast("/") }
                .subscribeOn(Schedulers.elastic())
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

    fun downloadItemCover(item: Item): Mono<Void> =
            wcb.clone()
                    .baseUrl(item.cover.url)
                    .build()
                    .get()
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .bodyToMono(ByteArrayResource::class.java)
                    .map { val file = p.rootfolder
                            .resolve(item.podcast.title)
                            .create()
                            .resolve("${item.id}.${item.cover.extension()}")

                        log.debug("Save file ${file.toAbsolutePath()}")
                        Files.write(file, it.byteArray) }
                    .then()

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
private fun CoverForPodcast.extension(): String {
    val ext = FilenameUtils.getExtension(url.toASCIIString())

    return if(ext.isBlank()) "jpg" else ext
}
private fun CoverForItem.extension() = FilenameUtils.getExtension(url) ?: "jpg"
