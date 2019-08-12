package com.github.davinkevin.podcastserver.manager.worker.rss

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.orElse
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.manager.worker.Finder
import com.github.davinkevin.podcastserver.service.CoverInformation
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty
import reactor.core.publisher.toMono
import java.io.ByteArrayInputStream
import java.net.URI

/**
 * Created by kevin on 22/02/15
 */
@Service
class RSSFinder(
        val imageService: ImageService,
        val wcb: WebClient.Builder
) : Finder {

    override fun find(url: String): Podcast {
        TODO("not required anymore") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findInformation(url: String): Mono<FindPodcastInformation> = wcb
            .baseUrl(url)
            .build()
            .get()
            .retrieve()
            .bodyToMono<String>()
            .map { SAXBuilder().build(ByteArrayInputStream(it.toByteArray(Charsets.UTF_8))) }
            .map { it.rootElement.getChild(CHANNEL) }
            .filter { it != null }
            .map { xmlToPodcast(it, url) }
            .onErrorResume {
                FindPodcastInformation(title = "", url = URI(url), type = "RSS", cover = null, description = "")
                        .toMono()
            }

    private fun xmlToPodcast(element: Element, anUrl: String) =
            FindPodcastInformation(
                type = "RSS",
                url = URI(anUrl),
                title = element.getChildText(TITLE),
                description = element.getChildText(DESCRIPTION),
                cover = coverUrlOf(element)?.toFindCover()
            )

    private fun coverUrlOf(channelElement: Element): CoverInformation? {
        return getRssImage(channelElement)
                .orElse { getItunesImage(channelElement) }
                .map { imageService.fetchCoverInformation(it) }
                .getOrElse{ null }
    }

    private fun getItunesImage(channelElement: Element) =
            Option.fromNullable(channelElement.getChild(IMAGE, JdomService.ITUNES_NAMESPACE))
                    .map { it.getAttributeValue(HREF) }

    private fun getRssImage(channelElement: Element) =
            Option.fromNullable(channelElement.getChild(IMAGE))
                    .map { it.getChildText(URL) }

    override fun compatibility(url: String?) = Integer.MAX_VALUE - 1

    companion object {
        private const val CHANNEL = "channel"
        private const val TITLE = "title"
        private const val DESCRIPTION = "description"
        private const val IMAGE = "image"
        private const val URL = "url"
        private const val HREF = "href"
    }
}

private fun CoverInformation.toFindCover() = FindCoverInformation(height, width, url)
