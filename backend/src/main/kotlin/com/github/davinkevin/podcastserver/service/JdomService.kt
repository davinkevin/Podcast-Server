package com.github.davinkevin.podcastserver.service

import arrow.core.Try
import arrow.core.getOrElse
import arrow.core.toOption
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.control.Option
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.entity.WatchList
import lan.dk.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.Text
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.Boolean.TRUE
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Objects.nonNull
import java.util.function.Function

@Service
class JdomService (val podcastServerParameters: PodcastServerParameters, val mimeTypeService: MimeTypeService, val urlService: UrlService){

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    fun parse(url: String): Option<Document> {

        val v = Try { urlService.asStream(url).use { SAXBuilder().build(it) } }
                .getOrElse { throw RuntimeException("Error during parsing of $url") }

        return v.toOption().toVΛVΓ()
    }

    fun podcastToXMLGeneric(podcast: Podcast, domainName: String, limit: Boolean?): String {
        return podcastToXMLGeneric(podcast, domainName, withNumberOfItem(podcast, limit))
    }

    private fun podcastToXMLGeneric(podcast: Podcast, domainName: String, limit: Long): String {
        val coverUrl = podcast.cover.relativeUrl(domainName)

        val channel = Element(CHANNEL).apply {
                addContent(Element(TITLE).addContent(Text(podcast.title)))
                addContent(Element(LINK).addContent(Text(String.format(LINK_PODCAST_FORMAT, domainName, podcast.id))))
                addContent(Element(DESCRIPTION).addContent(Text(podcast.description)))
                addContent(Element(SUBTITLE, ITUNES_NAMESPACE).addContent(Text(podcast.description)))
                addContent(Element(SUMMARY, ITUNES_NAMESPACE).addContent(Text(podcast.description)))
                addContent(Element(LANGUAGE).addContent(Text("fr-fr")))
                addContent(Element(AUTHOR, ITUNES_NAMESPACE).addContent(Text(podcast.type)))
                addContent(Element(CATEGORY, ITUNES_NAMESPACE))
        }

        if (podcast.lastUpdate != null) {
            val d = podcast.lastUpdate.format(DateTimeFormatter.RFC_1123_DATE_TIME)
            channel.addContent(Element(PUB_DATE).addContent(d))
        }

        if (podcast.cover != null) {
            val itunesImage = Element(IMAGE, ITUNES_NAMESPACE).apply { addContent(Text(coverUrl)) }

            val image = Element(IMAGE).apply {
                    addContent(Element(HEIGHT).addContent(podcast.cover.height.toString()))
                    addContent(Element(URL_STRING).addContent(coverUrl))
                    addContent(Element(WIDTH).addContent(podcast.cover.width.toString()))
            }

            channel
                    .addContent(image)
                    .addContent(itunesImage)
        }

        podcast.items
                .stream()
                .filter { nonNull(it.pubDate) }
                .sorted(PUB_DATE_COMPARATOR)
                .limit(limit)
                .map { it.toElement(domainName)}
                .forEachOrdered { channel.addContent(it) }

        return channelToRss(channel)
    }

    fun watchListToXml(watchList: WatchList, domainName: String): String {

        val channel = Element(CHANNEL).apply {
            addContent(Element(TITLE).addContent(Text(watchList.name)))
            addContent(Element(LINK).addContent(Text("$domainName/api/watchlists/${watchList.id}/rss")))
        }

        watchList.items
                .stream()
                .filter { nonNull(it.pubDate) }
                .sorted(PUB_DATE_COMPARATOR)
                .map { it.toElement(domainName)}
                .forEachOrdered { channel.addContent(it) }


        return channelToRss(channel);
    }

    private fun channelToRss(channel: Element): String {
        val rss = Element(RSS).apply {
            addContent(channel)
            addNamespaceDeclaration(ITUNES_NAMESPACE)
            addNamespaceDeclaration(MEDIA_NAMESPACE)
        }

        return XMLOutputter(Format.getPrettyFormat()).outputString(Document(rss))
    }

    fun podcastsToOpml(podcasts: List<Podcast>, domain: String): String {
        val opml = Element(OPML).apply {
            setAttribute("version", "2.0")
        }

        val head = Element(HEAD).apply {
            addContent(Element(TITLE).addContent("Podcast-Server"))
        }

        val outlines = podcasts
                .sortedBy { it.title }
                .map { it.toOutline(domain) }

        val body = Element(BODY)
                .addContent(outlines)

        val doc = with(opml) {
            addContent(head)
            addContent(body)
            Document(this)
        }

        return XMLOutputter(Format.getPrettyFormat())
                .outputString(doc)
    }

    companion object {

        // Element names :
        private val CHANNEL = "channel"
        private val TITLE = "title"
        private val LINK = "link"
        private val PUB_DATE = "pubDate"
        private val DESCRIPTION = "description"
        private val SUBTITLE = "subtitle"
        private val SUMMARY = "summary"
        private val LANGUAGE = "language"
        private val AUTHOR = "author"
        private val CATEGORY = "category"
        private val IMAGE = "image"
        private val URL_STRING = "url"
        private val WIDTH = "width"
        private val HEIGHT = "height"
        private val ITEM = "item"
        private val ENCLOSURE = "enclosure"
        private val LENGTH = "length"
        private val TYPE = "type"
        private val EXPLICIT = "explicit"
        private val NO = "No"
        private val GUID = "guid"
        private val THUMBNAIL = "thumbnail"
        private val RSS = "rss"
        private val OPML = "opml"
        private val HEAD = "head"
        private val BODY = "body"
        private val OUTLINE = "outline"
        private val TEXT = "text"
        private val HTML_URL = "htmlUrl"
        private val VERSION = "version"
        private val XML_URL = "xmlUrl"
        private val RSS_2 = "RSS2"

        //Useful namespace :
        val ITUNES_NAMESPACE = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd")
        private val MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/")

        // URL Format
        private val LINK_PODCAST_HTML_FORMAT = "%s/podcasts/%s"
        private val LINK_PODCAST_FORMAT = "%s/api/podcasts/%s/rss"
        private val PUB_DATE_COMPARATOR = { one: Item, another: Item -> -(one.pubDate.compareTo(another.pubDate)) }
        private val TITLE_COMPARATOR = Comparator.comparing(Function<Podcast, String> { it.title })
    }

    private fun Podcast.toOutline(domain: String) =
            Element(JdomService.OUTLINE).apply {
                setAttribute(JdomService.TEXT, title)
                setAttribute(JdomService.DESCRIPTION, if (description != null) description else "")
                setAttribute(JdomService.HTML_URL, "$domain/podcasts/${id}")
                setAttribute(JdomService.TITLE, title)
                setAttribute(JdomService.TYPE, JdomService.RSS)
                setAttribute(JdomService.VERSION, JdomService.RSS_2)
                setAttribute(JdomService.XML_URL, String.format(JdomService.LINK_PODCAST_FORMAT, domain, id))
            }

    private fun Item.toElement(domain: String): Element {
        val itemCoverUrl = coverOfItemOrPodcast.relativeUrl(domain)
        val itunesItemThumbnail = Element(IMAGE, ITUNES_NAMESPACE).setContent(Text(itemCoverUrl))
        val thumbnail = Element(THUMBNAIL, MEDIA_NAMESPACE).setAttribute(URL_STRING, itemCoverUrl)

        val itemEnclosure = Element(ENCLOSURE).apply {
            val ext = if (isDownloaded) "." + FilenameUtils.getExtension(fileName) else mimeTypeService.getExtension(this@toElement)
            setAttribute(URL_STRING, "$domain$proxyURLWithoutExtention$ext")
        }

        if(length != null) {
            itemEnclosure.setAttribute(LENGTH, length.toString())
        }

        if(mimeType?.isNotEmpty() == true) {
            itemEnclosure.setAttribute(TYPE, mimeType)
        }

        return Element(ITEM).apply {
            addContent(Element(TITLE).addContent(Text(title)))
            addContent(Element(DESCRIPTION).addContent(Text(description)))
            addContent(Element(PUB_DATE).addContent(Text(pubDate.format(DateTimeFormatter.RFC_1123_DATE_TIME))))
            addContent(Element(EXPLICIT, ITUNES_NAMESPACE).addContent(Text(NO)))
            addContent(Element(SUBTITLE, ITUNES_NAMESPACE).addContent(Text(title)))
            addContent(Element(SUMMARY, ITUNES_NAMESPACE).addContent(Text(description)))
            addContent(Element(GUID).addContent(Text("$domain$proxyURL")))
            addContent(itunesItemThumbnail)
            addContent(thumbnail)
            addContent(itemEnclosure)
        }
    }

    private fun withNumberOfItem(podcast: Podcast, limit: Boolean?) =
            if (TRUE == limit) podcastServerParameters.rssDefaultNumberItem
            else podcast.items.size.toLong()


    private fun Cover.relativeUrl(domain: String): String =
            if (url.startsWith("/")) "$domain$url"
            else url
}