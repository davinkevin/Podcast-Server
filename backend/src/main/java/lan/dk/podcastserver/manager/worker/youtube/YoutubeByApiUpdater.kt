package lan.dk.podcastserver.manager.worker.youtube

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.service.HtmlService
import io.vavr.collection.HashSet
import io.vavr.collection.Set
import io.vavr.control.Option
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.manager.worker.Type
import lan.dk.podcastserver.manager.worker.Updater
import lan.dk.podcastserver.manager.worker.youtube.YoutubeByApiUpdater.Companion.URL_PAGE_BASE
import lan.dk.podcastserver.service.JsonService
import lan.dk.podcastserver.service.properties.Api
import lombok.Getter
import lombok.Setter
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Created by kevin on 13/09/2018
 */
class YoutubeByApiUpdater(val htmlService: HtmlService, val jsonService: JsonService, val api: Api): Updater {

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    override fun getItems(podcast: Podcast): Set<Item> {
        log.info("Youtube Update by API")

        val playlistId = if (isPlaylist(podcast.url)) playlistIdOf(podcast.url) else transformChannelIdToPlaylistId(channelIdOf(htmlService, podcast.url))

        var nextPageToken: String? = null
        var items: Set<Item> = HashSet.empty()
        var pageItems: Set<Item> = HashSet.empty()
        var page = 0
        do {
            items = items.addAll(pageItems)

            val jsonResponse = jsonService
                    .parseUrl(asApiPlaylistUrl(playlistId, nextPageToken))
                    .map(JsonService.to(YoutubeApiResponse::class.java))

            pageItems = jsonResponse.map { it.items }
                    .map { convertToItems(it!!) }
                    .getOrElse{ HashSet.empty() }

            nextPageToken = jsonResponse.map { it.nextPageToken }
                    .getOrElse(StringUtils.EMPTY)
        } while (page++ < MAX_PAGE && StringUtils.isNotEmpty(nextPageToken))
        // Can't Access the podcast item here due thread-safe JPA / Hibernate problem
        // So, I choose to limit to 500 item / 10 Page of Youtube

        if (StringUtils.isEmpty(nextPageToken)) {
            items = items.addAll(pageItems)
        }

        return items
    }

    private fun transformChannelIdToPlaylistId(channelId: String) =
            if (channelId.startsWith("UC")) channelId.replaceFirst("UC".toRegex(), "UU")
            else channelId

    private fun asApiPlaylistUrl(playlistId: String, pageToken: String?): String {
        val url = API_PLAYLIST_URL.format(playlistId, api.youtube)
        return if (pageToken == null) url
        else "$url&pageToken=$pageToken"
    }

    private fun convertToItems(items: Set<YoutubeApiItem>): Set<Item> {
        return items.map { convertToItem(it) }
    }

    private fun convertToItem(item: YoutubeApiItem): Item {
        return Item().apply {
            title = item.title
            description = item.description
            pubDate = item.publishedAt
            url = item.url
            cover = item.cover
                    .map { Cover().apply {
                        url = it.url
                        width = it.width
                        height = it.height
                    } }
                    .getOrElse { Cover.DEFAULT_COVER }
        }
    }

    override fun signatureOf(podcast: Podcast?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun type(): Type {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compatibility(url: String?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private const val MAX_PAGE = 10
        private const val API_PLAYLIST_URL = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=%s&key=%s"
        internal const val URL_PAGE_BASE = "https://www.youtube.com/watch?v=%s"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal class YoutubeApiResponse {

    var items: Set<YoutubeApiItem>? = null
        set(items) {
            field = this.items
        }
    @Getter
    @Setter
    var nextPageToken: String? = null
        set(nextPageToken) {
            field = this.nextPageToken
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal class YoutubeApiItem {
    @Setter
    private val snippet: Snippet? = null

    val title: String?
        get() = snippet!!.title

    val description: String?
        get() = snippet!!.description

    internal//2013-12-20T22:30:01.000Z
    val publishedAt: ZonedDateTime
        get() = ZonedDateTime.parse(snippet!!.publishedAt!!, DateTimeFormatter.ISO_DATE_TIME)

    val url: String
        get() = String.format(URL_PAGE_BASE, snippet!!.resourceId!!.videoId)

    val cover: Option<Thumbnails.Thumbnail>
        get() =
            if (this.snippet!!.thumbnails == null) io.vavr.API.None<Thumbnails.Thumbnail>()
            else this.snippet.thumbnails!!.betterThumbnail

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class Snippet {
        @Getter
        @Setter
        var title: String? = null
            set(title) {
                field = this.title
            }
        @Getter
        @Setter
        var description: String? = null
            set(description) {
                field = this.description
            }
        @Getter
        @Setter
        var publishedAt: String? = null
            set(publishedAt) {
                field = this.publishedAt
            }
        @Getter
        @Setter
        var thumbnails: Thumbnails? = null
            set(thumbnails) {
                field = this.thumbnails
            }
        @Getter
        @Setter
        var resourceId: ResourceId? = null
            set(resourceId) {
                field = this.resourceId
            }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private class ResourceId {
        @Getter
        @Setter
        var videoId: String? = null
            set(videoId) {
                field = this.videoId
            }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    internal class Thumbnails {
        @Setter
        private val maxres: Thumbnail? = null
        @Setter
        private val standard: Thumbnail? = null
        @Setter
        private val high: Thumbnail? = null
        @Setter
        private val medium: Thumbnail? = null
        @Setter
        @JsonProperty("default")
        private val byDefault: Thumbnail? = null

        internal val betterThumbnail: Option<Thumbnail>
            get() {
                if (maxres != null)
                    return io.vavr.API.Option<Thumbnail>(maxres)

                if (standard != null)
                    return io.vavr.API.Option<Thumbnail>(standard)

                if (high != null)
                    return io.vavr.API.Option<Thumbnail>(high)

                if (medium != null)
                    return io.vavr.API.Option<Thumbnail>(medium)

                return if (byDefault != null) io.vavr.API.Option<Thumbnail>(byDefault)
                else io.vavr.API.None<Thumbnail>()

            }

        @JsonIgnoreProperties(ignoreUnknown = true)
        class Thumbnail {
            @Getter
            @Setter
            var url: String? = null
                set(url) {
                    field = this.url
                }
            @Getter
            @Setter
            var width: Int? = null
                set(width) {
                    field = this.width
                }
            @Getter
            @Setter
            var height: Int? = null
                set(height) {
                    field = this.height
                }
        }
    }
}

