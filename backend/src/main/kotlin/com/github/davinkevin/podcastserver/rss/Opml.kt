package com.github.davinkevin.podcastserver.rss

import org.jdom2.Element
import java.net.URI
import java.util.*


data class Opml(val podcastOutlines: List<OpmlOutline>) {

    fun toElement(): Element {
        return Element("opml").apply {
            setAttribute("version", "2.0")

            val head = Element("head").apply {
                addContent(Element("title").addContent("Podcast-Server"))
            }

            val outlines = podcastOutlines
                .sortedBy { it.podcast.title }
                .map { it.toXML() }

            val body = Element("body")
                .addContent(outlines)

            addContent(head)
            addContent(body)
        }
    }
}

data class OpmlOutline(val podcast: Podcast, val host: URI) {
    data class Podcast(val id: UUID, val title: String, val description: String?)

    fun toXML(): Element {

        val htmlUrl = host.toASCIIString()!! + "podcasts/${podcast.id}"
        val xmlUrl = host.toASCIIString()!! + "api/podcasts/${podcast.id}/rss"

        return Element("outline").apply {
            setAttribute("text", podcast.title)
            setAttribute("description", podcast.description ?: "")
            setAttribute("htmlUrl", htmlUrl)
            setAttribute("title", podcast.title)
            setAttribute("type", "rss")
            setAttribute("version", "RSS2")
            setAttribute("xmlUrl", xmlUrl)
        }
    }
}
