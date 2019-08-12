package com.github.davinkevin.podcastserver.manager.worker.rss

import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.manager.worker.Finder
import com.github.davinkevin.podcastserver.service.CoverInformation
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
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
                title = element.getChildText("title"),
                description = element.getChildText("description"),
                cover = coverUrlOf(element)?.toFindCover()
            )

    private fun coverUrlOf(channelElement: Element): CoverInformation? {
        val rss = channelElement.getChild("image")?.getChildText("url")
        val itunes = channelElement.getChild("image", itunesNS)?.getAttributeValue("href")

        val url = rss ?: itunes ?: return null

        return imageService.fetchCoverInformation(url)
    }

    override fun compatibility(url: String?) = Integer.MAX_VALUE - 1
}

private fun CoverInformation.toFindCover() = FindCoverInformation(height, width, url)
