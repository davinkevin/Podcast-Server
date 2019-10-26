package com.github.davinkevin.podcastserver.find.finders.rss

import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.orNull
import com.github.davinkevin.podcastserver.find.toMonoOption
import com.github.davinkevin.podcastserver.manager.worker.Finder
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.io.ByteArrayInputStream
import java.net.URI
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

/**
 * Created by kevin on 22/02/15
 */
class RSSFinder(
        private val imageService: ImageService,
        private val wcb: WebClient.Builder
) : Finder {

    private val itunesNS = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")!!

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
            .map { it.rootElement.getChild("channel") }
            .flatMap { elem -> findCover(elem).map { it.toFindCover() }.toMonoOption().zipWith(elem.toMono()) }
            .map { (cover, elem) -> toPodcast(elem, url, cover.orNull()) }

    private fun toPodcast(element: Element, anUrl: String, cover: FindCoverInformation?) = FindPodcastInformation(
            type = "RSS",
            url = URI(anUrl),
            title = element.getChildText("title"),
            description = element.getChildText("description"),
            cover = cover
    )

    private fun findCover(channelElement: Element): Mono<CoverInformation> {
        val rss = channelElement.getChild("image")?.getChildText("url")
        val itunes = channelElement.getChild("image", itunesNS)?.getAttributeValue("href")

        val url = rss ?: itunes ?: return Mono.empty()

        return imageService.fetchCoverInformation(URI(url))
    }

    override fun compatibility(url: String?) = Integer.MAX_VALUE - 1
}

private fun CoverInformation.toFindCover() = FindCoverInformation(height, width, url)
