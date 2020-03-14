package com.github.davinkevin.podcastserver.find.finders.rss

import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.fetchCoverInformationOrOption
import com.github.davinkevin.podcastserver.find.finders.Finder
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
import java.util.*
import com.github.davinkevin.podcastserver.service.image.ImageService

/**
 * Created by kevin on 22/02/15
 */
class RSSFinder(
        private val imageService: ImageService,
        private val wcb: WebClient.Builder
) : Finder {

    private val itunesNS = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")!!

    override fun findInformation(url: String): Mono<FindPodcastInformation> = wcb
            .clone()
            .baseUrl(url)
            .build()
            .get()
            .retrieve()
            .bodyToMono<String>()
            .map { SAXBuilder().build(ByteArrayInputStream(it.toByteArray(Charsets.UTF_8))) }
            .map { it.rootElement.getChild("channel") }
            .flatMap { findCover(it).zipWith(it.toMono()) }
            .map { (cover, elem) -> FindPodcastInformation(
                    type = "RSS",
                    url = URI(url),
                    title = elem.getChildText("title"),
                    description = elem.getChildText("description"),
                    cover = cover.orNull()
            ) }

    private fun findCover(channelElement: Element): Mono<Optional<FindCoverInformation>> {
        val rss = channelElement.getChild("image")?.getChildText("url")
        val itunes = channelElement.getChild("image", itunesNS)?.getAttributeValue("href")

        val url = rss ?: itunes ?: return Optional.empty<FindCoverInformation>().toMono()

        return imageService.fetchCoverInformationOrOption(URI(url))
    }

    override fun compatibility(url: String?) = Integer.MAX_VALUE - 1
}
