package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation
import com.github.davinkevin.podcastserver.item.DeleteItemInformation
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.podcast.CoverForPodcast
import com.github.davinkevin.podcastserver.podcast.DeletePodcastInformation
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
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
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
            .clone()
            .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))

    fun deletePodcast(podcast: DeletePodcastInformation) = Mono.defer {
        val file = p.rootfolder.resolve(podcast.title)
        log.info("Deletion of podcast {}", podcast.title)

        Files.walk(file)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }

        Files.deleteIfExists(file)
                .toMono()
                .then()
    }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun deleteItem(item: DeleteItemInformation) = Mono.defer {
        val file = p.rootfolder.resolve(item.path)
        log.info("Deletion of file {}", file)
        Files
                .deleteIfExists(file)
                .toMono()
    }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun deleteCover(cover: DeleteCoverInformation): Mono<Boolean> = Mono.defer {
        val file = p.rootfolder
                .resolve(cover.podcast.title)
                .resolve(cover.item.id.toString() + "." + cover.extension)

        log.info("Deletion of file {}", file)

        Files.deleteIfExists(file).toMono()
    }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

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
    }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun downloadPodcastCover(podcast: Podcast): Mono<Void> {
        return wcb.clone()
                .baseUrl(podcast.cover.url.toASCIIString())
                .build()
                .get()
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(ByteArrayResource::class.java)
                .flatMap { val file = p.rootfolder
                            .resolveOrCreate(podcast.title)
                            .resolve("${podcast.id}.${podcast.cover.extension()}")

                    log.debug("Save file ${file.toAbsolutePath()}")
                    Mono.defer { Files.write(file, it.byteArray).toMono() }
                            .subscribeOn(Schedulers.boundedElastic())
                            .publishOn(Schedulers.parallel())
                }
                .then()
    }

    fun downloadItemCover(item: Item): Mono<Void> =
            wcb.clone()
                    .baseUrl(item.cover.url.toASCIIString())
                    .build()
                    .get()
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .bodyToMono<ByteArrayResource>()
                    .flatMap {
                        val file = p.rootfolder
                            .resolveOrCreate(item.podcast.title)
                            .resolve("${item.id}.${item.cover.extension()}")

                        log.debug("Save file ${file.toAbsolutePath()}")
                        Mono.defer { Files.write(file, it.byteArray).toMono() }
                                .subscribeOn(Schedulers.boundedElastic())
                                .publishOn(Schedulers.parallel())
                    }
                    .then()

    fun movePodcast(details: MovePodcastDetails): Mono<Void> = Mono.defer {
        val oldLocation = p.rootfolder.resolve(details.from)
        val newLocation = p.rootfolder.resolve(details.to)

        log.info("Move podcast from $oldLocation to $newLocation")

        Files.move(oldLocation, newLocation).toMono()
    }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())
            .then()

    fun upload(destination: Path, file: FilePart): Mono<Void> = Mono.defer {
        Files.deleteIfExists(destination)
        Files.createDirectories(destination.parent)
        file.transferTo(destination).toMono()
    }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun size(file: Path): Mono<Long> = Mono.defer { Files.size(file).toMono() }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())

    fun probeContentType(file: Path): Mono<String> = Mono.defer { mimeTypeService.probeContentType(file).toMono() }
            .subscribeOn(Schedulers.boundedElastic())
            .publishOn(Schedulers.parallel())
}

private fun Path.resolveOrCreate(title: String): Path {
    val folder = this.resolve(title)
    return if (Files.exists(folder)) folder else Files.createDirectory(folder)
}
private fun CoverForPodcast.extension(): String {
    val ext = FilenameUtils.getExtension(url.toASCIIString())

    return if(ext.isBlank()) "jpg" else ext
}
private fun Item.Cover.extension() = FilenameUtils.getExtension(url.toASCIIString()) ?: "jpg"

data class MovePodcastDetails(val id: UUID, val from: String, val to: String)
