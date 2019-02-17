package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.extension.ServerRequest.extractHost
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.seeOther
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import reactor.core.publisher.toMono
import java.net.URI
import java.nio.file.Paths
import java.util.*

/**
 * Created by kevin on 2019-02-09
 */
@Component
class ItemHandler(val itemService: ItemService, val fileService: FileService) {

    private var log = LoggerFactory.getLogger(ItemHandler::class.java)

    fun clean(s: ServerRequest) =
            itemService
                    .deleteOldEpisodes()
                    .flatMap { ok().build() }

    fun file(s: ServerRequest): Mono<ServerResponse> {
        val host = s.extractHost()
        val id = UUID.fromString(s.pathVariable("id"))

        return itemService.findById(id)
                .flatMap { item -> item
                            .toMono()
                            .filter(Item::isDownloaded)
                            .map { UriComponentsBuilder.fromUri(host)
                                    .pathSegment("data", it.podcast.title, it.fileName)
                                    .build().toUri() }
                            .switchIfEmpty { URI(item.url).toMono() }
                }
                .doOnNext { log.debug("Redirect content playabe to {}", it)}
                .flatMap { seeOther(it).build() }
    }

    fun cover(s: ServerRequest): Mono<ServerResponse> {
        val host = s.extractHost()
        val id = UUID.fromString(s.pathVariable("id"))
        return itemService.findById(id)
                .doOnNext { log.debug("the url of cover is ${it.cover.url}")}
                .flatMap { item -> item
                        .toMono()
                        .map { Paths.get(it.podcast.title).resolve("${it.id}.${it.cover.extension()}") }
                        .flatMap { fileService.exists(it) }
                        .map { it.toString().substringAfterLast("/") }
                        .map { UriComponentsBuilder.fromUri(host)
                                    .pathSegment("data", item.podcast.title, it)
                                    .build().toUri()
                        }
                        .switchIfEmpty { URI(item.cover.url).toMono() }
                }
                .doOnNext { log.debug("Redirect cover to {}", it)}
                .flatMap { seeOther(it).build() }
    }
}

private fun CoverForItem.extension() = FilenameUtils.getExtension(url) ?: "jpg"
