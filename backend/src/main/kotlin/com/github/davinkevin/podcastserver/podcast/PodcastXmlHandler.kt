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
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.paramOrNull
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.util.*
import com.github.davinkevin.podcastserver.rss.Item as RssItem

class PodcastXmlHandler(
    private val podcastService: PodcastService,
    private val itemService: ItemService
) {

    fun opml(r: ServerRequest): ServerResponse {
        val host = r.extractHost()

        val podcasts = podcastService.findAll().collectList().block()!!

        val outlines = podcasts
            .map { OpmlOutline(OpmlOutline.Podcast(it.id, it.title, it.description), host) }
            .sortedBy { it.podcast.title }

        val opml = Opml(outlines).toElement()

        val xml = XMLOutputter(Format.getPrettyFormat()).outputString(Document(opml))

        return ServerResponse.ok().contentType(MediaType.APPLICATION_XML).body(xml)
    }

    fun rss(r: ServerRequest): ServerResponse {
        val host = r.extractHost()
        val callUrl = r.normalizedURI()
        val podcastId = UUID.fromString(r.pathVariable("id"))

        val queryLimit = r.paramOrNull("limit") ?: "true"
        val limit = queryLimit.toLimit()
        val itemPageable = ItemPageRequest(0, limit, ItemSort("DESC", "pubDate"))

        val (page, podcast) = Mono.zip(
            itemService.search(
                q = "",
                tags = listOf(),
                status = listOf(),
                page = itemPageable,
                podcastId = podcastId
            ),
            podcastService.findById(podcastId)
        ).block()!!

        val items = page.content.map { it.toRssItem(host) }
        val rss = podcast.toRssChannel(callUrl)
            .addContent(items)
            .let(::rootRss)

        val xml = XMLOutputter(Format.getPrettyFormat())
            .outputString(Document(rss))

        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(xml)
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

    return when (isBoolean.getOrThrow()) {
        true -> defaultLimit
        false -> Int.MAX_VALUE
    }
}
