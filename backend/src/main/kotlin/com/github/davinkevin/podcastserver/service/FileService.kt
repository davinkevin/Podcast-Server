package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.item.ItemService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by kevin on 2019-02-09
 */
@Service
class FileService(val p: PodcastServerParameters) {

    private val log = LoggerFactory.getLogger(FileService::class.java)

    fun deleteItem(path: Path) = Mono.defer {
        val file = p.rootfolder.resolve(path)
        log.info("Deletion of file {}", file)
        Files
                .deleteIfExists(file)
                .toMono()
    }

    fun exists(path: Path) = Mono.defer {
        val file = p.rootfolder.resolve(path)
        val exists = Files.exists(file)
        log.debug("the file $file exists: $exists")
        exists
                .toMono()
                .filter { it }
                .map { path }
    }
}
