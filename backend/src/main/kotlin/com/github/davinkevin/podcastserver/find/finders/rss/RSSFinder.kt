package com.github.davinkevin.podcastserver.find.finders.rss

import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.fetchFindCoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.io.StringReader
import java.net.URI

/**
 * Created by kevin on 22/02/15
 */
class RSSFinder(
        private val imageService: ImageService,
        private val rcb: RestClient.Builder
) : Finder {

    private val itunesNS = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")!!

    override fun findPodcastInformation(url: String): FindPodcastInformation? {
        val content = rcb
            .clone()
            .baseUrl(url)
            .build()
            .get()
            .retrieve()
            .body<String>()
            ?: return null

        val channel = SAXBuilder().build(StringReader(content))
            .rootElement
            .getChild("channel")

        val cover = findCover(channel)

        return FindPodcastInformation(
            type = "RSS",
            url = URI(url),
            title = channel.getChildText("title"),
            description = channel.getChildText("description"),
            cover = cover
        )
    }

    private fun findCover(channelElement: Element): FindCoverInformation? {
        val rss = channelElement.getChild("image")?.getChildText("url")
        val itunes = channelElement.getChild("image", itunesNS)?.getAttributeValue("href")

        val url = rss ?: itunes ?: return null

        return imageService.fetchFindCoverInformation(URI(url))
    }

    override fun compatibility(url: String): Int = Integer.MAX_VALUE - 1
}
