package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.github.davinkevin.podcastserver.find.finders.meta
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Updater
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jsoup.Jsoup
import org.springframework.core.io.ByteArrayResource
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")

class YoutubeByXmlUpdater(
    private val youtube: RestClient
): Updater {

    override fun findItems(podcast: PodcastToUpdate): List<ItemFromUpdate> {
        val page = fetchXml(podcast.url)
            ?: return emptyList()

        val xml = SAXBuilder().build(page.inputStream)
        val dn = xml.rootElement.namespace

        return xml
            .rootElement
            .getChildren("entry", dn)
            .map { toItem(it, dn) }
    }

    private fun toItem(e: Element, dn: Namespace): ItemFromUpdate {
        val mediaGroup = e.getChild("group", MEDIA_NAMESPACE)
        val idVideo = mediaGroup.getChild("content", MEDIA_NAMESPACE)
            .getAttributeValue("url")
            .substringAfterLast("/")
            .substringBefore("?")

        val thumbnail = mediaGroup.getChild("thumbnail", MEDIA_NAMESPACE)

        return ItemFromUpdate(
            title = e.getChildText("title", dn),
            description = mediaGroup.getChildText("description", MEDIA_NAMESPACE),

            //2013-12-20T22:30:01.000Z
            pubDate = ZonedDateTime.parse(e.getChildText("published", dn), DateTimeFormatter.ISO_DATE_TIME),

            url = URI("https://www.youtube.com/watch?v=$idVideo"),
            cover = ItemFromUpdate.Cover(
                url = URI(thumbnail.getAttributeValue("url")),
                width = thumbnail.getAttributeValue("width").toInt(),
                height = thumbnail.getAttributeValue("height").toInt()
            ),
            mimeType = "video/webm"
        )
    }

    override fun signatureOf(url: URI): String {
        val page = fetchXml(url)
            ?: return ""

        val xml = SAXBuilder().build(page.inputStream)
        val dn = xml.rootElement.namespace
        val items = xml
            .rootElement
            .getChildren("entry", dn)
            .map { it.getChildText("id", dn) }

        if(items.isEmpty()) {
            return ""
        }

        return items
            .sorted()
            .reduce { t, u -> "$t, $u" }
            .let(String::toByteArray)
            .let(DigestUtils::md5DigestAsHex)
    }

    private fun fetchXml(url: URI): ByteArrayResource? {
        val (key, value) = queryParamsOf(url)
            ?: return null

        return youtube
            .get()
            .uri { it
                .path("/feeds/videos.xml")
                .queryParam(key, value)
                .build()
            }
            .retrieve()
            .body<ByteArrayResource>()
    }

    private fun queryParamsOf(url: URI): Pair<String, String>? {
        val stringUrl = url.toASCIIString()

        if (isPlaylist(url)) {
            val playlistId = stringUrl.substringAfter("list=")
            return "playlist_id" to playlistId
        }

        val path = stringUrl.substringAfterLast("https://www.youtube.com")
        val page = youtube
            .get()
            .uri(path)
            .retrieve()
            .body<String>()
            ?: return null

        val html = Jsoup.parse(page, "https://www.youtube.com")

        return "channel_id" to html.meta("itemprop=identifier")
    }

    override fun type() = type
    override fun compatibility(url: String): Int = youtubeCompatibility(url)
}
