package com.github.davinkevin.podcastserver.manager.worker.rss

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.orElse
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Finder
import org.jdom2.Element
import org.springframework.stereotype.Service

/**
 * Created by kevin on 22/02/15
 */
@Service
class RSSFinder(val jdomService: JdomService, val imageService: ImageService) : Finder {

    override fun find(url: String) =
            jdomService
                    .parse(url)
                    .map { it.rootElement.getChild(CHANNEL) }
                    .filter { it != null }
                    .map { xmlToPodcast(it, url) }
                    .getOrElse(Podcast.DEFAULT_PODCAST)

    private fun xmlToPodcast(element: Element, anUrl: String) =
            Podcast().apply {
                type = "RSS"
                url = anUrl
                title = element.getChildText(TITLE)
                description = element.getChildText(DESCRIPTION)
                cover = coverUrlOf(element)
            }

    private fun coverUrlOf(channelElement: Element) =
            getRssImage(channelElement)
                    .orElse { getItunesImage(channelElement) }
                    .map { imageService.getCoverFromURL(it) }
                    .getOrElse{ Cover.DEFAULT_COVER }!!

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
