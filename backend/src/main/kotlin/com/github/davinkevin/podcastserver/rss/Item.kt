package com.github.davinkevin.podcastserver.rss

import com.github.davinkevin.podcastserver.extension.podcastserver.item.Sluggable
import org.jdom2.Element
import org.jdom2.Text
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.extension

data class Item(
    val host: URI,
    val podcast: Podcast,
    val item: Item,
    val cover: Cover,
) {

    data class Podcast(val id: UUID)
    data class Cover(val url: URI)
    data class Item(
        val id: UUID,
        override val title: String,
        override val mimeType: String,
        override val fileName: Path?,
        val pubDate: OffsetDateTime?,
        val description: String?,
        val length: Long?,
    ): Sluggable

    fun toElement(): Element {
        val itemUrlBuilder: UriComponentsBuilder = UriComponentsBuilder.fromUri(host)
            .pathSegment("api", "v1", "podcasts", podcast.id.toString(), "items", item.id.toString())

        val coverExtension = (Path(cover.url.path).extension.ifBlank { "jpg" })
        val coverUrl = itemUrlBuilder
            .cloneBuilder()
            .pathSegment("cover.$coverExtension")
            .build(true)
            .toUriString()

        val itunesItemThumbnail = Element("image", itunesNS).setContent(Text(coverUrl))
        val thumbnail = Element("thumbnail", mediaNS).setAttribute("url", coverUrl)

        val proxyURL = itemUrlBuilder
            .cloneBuilder()
            .pathSegment(item.slug())
            .build(true)
            .toUriString()

        val itemEnclosure = Element("enclosure").apply {
            setAttribute("url", proxyURL)

            if(item.length != null) {
                setAttribute("length", item.length.toString())
            }

            if(item.mimeType.isNotEmpty()) {
                setAttribute("type", item.mimeType)
            }
        }

        val guid = itemUrlBuilder
            .cloneBuilder()
            .build(true)
            .toUriString()

        return Element("item").apply {
            addContent(Element("title").addContent(Text(item.title)))
            addContent(Element("description").addContent(Text(item.description)))
            addContent(Element("pubDate").addContent(Text(item.pubDate!!.format(DateTimeFormatter.RFC_1123_DATE_TIME))))
            addContent(Element("explicit", itunesNS).addContent(Text("No")))
            addContent(Element("subtitle", itunesNS).addContent(Text(item.title)))
            addContent(Element("summary", itunesNS).addContent(Text(item.description)))
            addContent(Element("guid").addContent(Text(guid)))
            addContent(itunesItemThumbnail)
            addContent(thumbnail)
            addContent(itemEnclosure)
        }

    }
}