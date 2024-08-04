package com.github.davinkevin.podcastserver.rss

import org.jdom2.Element
import org.jdom2.Text
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.extension

data class PlaylistChannel(
    val playlist: Playlist,
    val cover: Cover,
    val calledURI: URI,
) {
    data class Playlist(val id: UUID, val name: String)
    data class Cover(val url: URI, val height: Int, val width: Int)

    fun toElement(): Element {
        val url = UriComponentsBuilder.fromUri(calledURI).build(true).toUriString()
        val baseUrl = cover.url

        return Element("channel").apply {
            addContent(Element("title").addContent(playlist.name))
            addContent(Element("link").addContent(url))

            addContent(Element("image", itunesNS).addContent(baseUrl.toASCIIString()))

            addContent(Element("image").apply {
                addContent(Element("height").addContent(cover.height.toString()))
                addContent(Element("url").addContent(baseUrl.toASCIIString()))
                addContent(Element("width").addContent(cover.width.toString()))
            })
        }
    }
}

data class PodcastChannel(
    val podcast: Podcast,
    val cover: Cover,
    val calledURI: URI,
) {
    data class Podcast(val id: UUID, val title: String, val description: String?, val type: String, val lastUpdate: OffsetDateTime?)
    data class Cover(val url: URI, val height: Int, val width: Int)

    fun toElement(): Element {
        val url = UriComponentsBuilder.fromUri(calledURI).build(true).toUriString()
        val baseUrl = calledURI.scheme + "://" + calledURI.authority

        val coverUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "cover." + Path(cover.url.path).extension)
            .build(true)
            .toUriString()

        return Element("channel").apply {
            addContent(Element("title").addContent(Text(podcast.title)))
            addContent(Element("link").addContent(Text(url)))
            addContent(Element("description").addContent(Text(podcast.description)))
            addContent(Element("subtitle", itunesNS).addContent(Text(podcast.description)))
            addContent(Element("summary", itunesNS).addContent(Text(podcast.description)))
            addContent(Element("language").addContent(Text("fr-fr")))
            addContent(Element("author", itunesNS).addContent(Text(podcast.type)))
            addContent(Element("category", itunesNS))

            if (podcast.lastUpdate != null) {
                val date = podcast.lastUpdate.format(DateTimeFormatter.RFC_1123_DATE_TIME)
                addContent(Element("pubDate").addContent(date))
            }

            addContent(Element("image", itunesNS).addContent(Text(coverUrl)))

            addContent(Element("image").apply {
                addContent(Element("height").addContent(cover.height.toString()))
                addContent(Element("url").addContent(coverUrl))
                addContent(Element("width").addContent(cover.width.toString()))
            })
        }
    }
}