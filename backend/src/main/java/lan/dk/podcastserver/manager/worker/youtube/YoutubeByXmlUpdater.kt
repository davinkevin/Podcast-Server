package lan.dk.podcastserver.manager.worker.youtube

import arrow.core.Option
import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Type
import lan.dk.podcastserver.manager.worker.Updater
import org.jdom2.Element
import org.jdom2.Namespace
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Objects.nonNull

/**
 * Created by kevin on 11/09/2018
 */
class YoutubeByXmlUpdater(val jdomService: JdomService, val htmlService: HtmlService, val signatureService: SignatureService) : Updater {

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    override fun getItems(podcast: Podcast): io.vavr.collection.Set<Item> {
        log.info("Youtube Update by RSS")

        val url = xmlUrlOf(podcast.url)
        val parsedXml = jdomService.parse(url).k()

        val dn = parsedXml
                .map { it.rootElement }
                .map { it.namespace }
                .getOrElse { Namespace.NO_NAMESPACE }

        return parsedXml
                .map { it.rootElement }
                .map { it.getChildren("entry", it.namespace) }
                .getOrElse { listOf() }
                .map { toItem(it, dn) }
                .toSet()
                .toVΛVΓ()
    }

    private fun isPlaylist(url: String) =
            url.contains(PLAYLIST_URL_PART)

    private fun xmlUrlOf(url: String): String =
            if (isPlaylist(url)) PLAYLIST_RSS_BASE.format(playlistIdOf(url))
            else CHANNEL_RSS_BASE.format(channelIdOf(url))

    private fun channelIdOf(url: String) =
            htmlService
                    .get(url).k()
                    .flatMap { it.select("[data-channel-external-id]").firstOption() }
                    .filter { nonNull(it) }
                    .map { it.attr("data-channel-external-id") }
                    .getOrElse { "" }

    private fun toItem(e: Element, dn: Namespace): Item {
        val mediaGroup = e.getChild("group", MEDIA_NAMESPACE)
        return Item().apply {
            title = e.getChildText("title", dn)
            description = mediaGroup.getChildText("description", MEDIA_NAMESPACE)

            //2013-12-20T22:30:01.000Z
            pubDate = ZonedDateTime.parse(e.getChildText("published", dn), DateTimeFormatter.ISO_DATE_TIME)

            url = urlOf(mediaGroup.getChild("content", MEDIA_NAMESPACE).getAttributeValue("url"))
            cover = coverOf(mediaGroup.getChild("thumbnail", MEDIA_NAMESPACE))
        }
    }

    private fun coverOf(thumbnail: Element?) =
            Option.fromNullable(thumbnail)
                    .map { Cover().apply {
                        url = it.getAttributeValue("url")
                        width = it.getAttributeValue("width").toInt()
                        height = it.getAttributeValue("height").toInt()
                    } }
                    .getOrElse { Cover.DEFAULT_COVER }

    private fun urlOf(embeddedVideoPage: String): String {
        val idVideo = embeddedVideoPage
                .substringAfterLast("/")
                .substringBefore("?")

        return URL_PAGE_BASE.format(idVideo)
    }


    private fun playlistIdOf(url: String): String {
        // TODO  : Use Pattern Match to extract PlaylistID in Feed case and url case
        return url.substringAfter("list=")
    }

    override fun signatureOf(podcast: Podcast): String {
        val url = xmlUrlOf(podcast.url)
        val parsedXml = jdomService.parse(url).k()

        val dn = parsedXml
                .map { it.rootElement }
                .map { it.namespace }
                .getOrElse { Namespace.NO_NAMESPACE }

        val joinedIds = parsedXml
                .map { it.rootElement }
                .map { it.getChildren("entry", it.namespace) }
                .getOrElse { listOf() }
                .joinToString { it.getChildText("id", dn) }

        return if(joinedIds.isEmpty()) joinedIds
        else signatureService.fromText(joinedIds)
    }


    override fun type(): Type {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compatibility(url: String?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private const val PLAYLIST_URL_PART = "www.youtube.com/playlist?list="
        private const val PLAYLIST_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?playlist_id=%s"
        private const val CHANNEL_RSS_BASE = "https://www.youtube.com/feeds/videos.xml?channel_id=%s"
        private const val URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s"
        private val MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")
    }

}