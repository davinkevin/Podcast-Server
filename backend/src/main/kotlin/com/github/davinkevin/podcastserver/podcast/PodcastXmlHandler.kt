package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import com.github.davinkevin.podcastserver.extension.serverRequest.normalizedURI
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.item.ItemPageRequest
import com.github.davinkevin.podcastserver.item.ItemService
import com.github.davinkevin.podcastserver.item.ItemSort
import com.github.davinkevin.podcastserver.rss.Opml
import com.github.davinkevin.podcastserver.rss.OpmlOutline
import com.github.davinkevin.podcastserver.rss.PodcastChannel
import com.github.davinkevin.podcastserver.rss.rootRss
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.queryParamOrNull
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.util.*
import com.github.davinkevin.podcastserver.rss.Item as RssItem

class PodcastXmlHandler(
    private val podcastService: PodcastService,
    private val itemService: ItemService
) {

    fun opml(r: ServerRequest): Mono<ServerResponse> {
        val host = r.extractHost()
        return podcastService.findAll()
            .map { OpmlOutline(OpmlOutline.Podcast(it.id, it.title, it.description), host) }
            .sort { first, second -> first.podcast.title.compareTo(second.podcast.title) }
            .collectList()
            .map { Opml(it).toElement() }
            .map { XMLOutputter(Format.getPrettyFormat()).outputString(Document(it)) }
            .flatMap { ServerResponse.ok().contentType(MediaType.APPLICATION_XML).bodyValue(it) }
    }

    fun rss(r: ServerRequest): Mono<ServerResponse> {
        val baseUrl = r.extractHost()
        val callUrl = r.normalizedURI()
        val podcastId = UUID.fromString(r.pathVariable("id"))

        val queryLimit = r.queryParamOrNull("limit") ?: "true"
        val limit = queryLimit.toLimit()
        val itemPageable = ItemPageRequest(0, limit, ItemSort("DESC", "pubDate"))

        val items = itemService.search(
            q = "",
            tags = listOf(),
            status = listOf(),
            page = itemPageable,
            podcastId = podcastId
        )
            .flatMapMany { it.content.toFlux() }
            .map { it.toRssItem(baseUrl) }
            .collectList()

        val podcast = podcastService
            .findById(podcastId)
            .map { it.toRssChannel(callUrl) }

        return Mono
            .zip(items, podcast)
            .map { (itemRss, podcastRss) -> podcastRss.addContent(itemRss) }
            .map(::rootRss)
            .map { XMLOutputter(Format.getPrettyFormat()).outputString(Document(it)) }
            .flatMap { ServerResponse.ok().contentType(MediaType.APPLICATION_XML).bodyValue(it) }
    }


}

private fun Item.toRssItem(host: URI): Element = RssItem(
    host = host,
    podcast = RssItem.Podcast(podcast.id),
    item = RssItem.Item(
        id = id,
        title = title,
        mimeType = mimeType,
        fileName = fileName,
        pubDate = pubDate,
        description = description,
        length = length,
    ),
    cover = RssItem.Cover(
        url = cover.url
    )
)
    .toElement()

private fun Podcast.toRssChannel( callUrl: URI): Element = PodcastChannel(
    podcast = PodcastChannel.Podcast(
        id = id,
        title = title,
        description = description,
        type = type,
        lastUpdate = lastUpdate,
    ),
    cover = PodcastChannel.Cover(
        url = cover.url,
        height = cover.height,
        width = cover.width,
    ),
    calledURI = callUrl,
).toElement()

private fun String.toLimit(): Int {
    val defaultLimit = 50

    val isBoolean = runCatching { toBooleanStrict() }

    if (isBoolean.isFailure) return toInt()

    return when (isBoolean.getOrDefault(true)) {
        true -> defaultLimit
        false -> Int.MAX_VALUE
    }
}
