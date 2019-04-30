package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.item.CoverForItem
import com.github.davinkevin.podcastserver.item.DeleteItemInformation
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.podcast.CoverForPodcast
import com.github.davinkevin.podcastserver.podcast.Podcast
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by kevin on 2019-02-09
 */
@Service
class FileService(val p: PodcastServerParameters) {

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
}

private fun CoverForPodcast.extension() = FilenameUtils.getExtension(url.toASCIIString()) ?: "jpg"
private fun CoverForItem.extension() = FilenameUtils.getExtension(url) ?: "jpg"
