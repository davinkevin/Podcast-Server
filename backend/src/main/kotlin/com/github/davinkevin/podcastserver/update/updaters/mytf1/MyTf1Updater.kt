package com.github.davinkevin.podcastserver.update.updaters.mytf1

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.fetchCoverUpdateInformation
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import com.github.davinkevin.podcastserver.utils.MatcherExtractor
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by kevin on 11/03/2020
 */
class MyTf1Updater(
    private val rc: RestClient,
    private val om: ObjectMapper,
    private val image: ImageService
): Updater {

    override fun findItems(podcast: PodcastToUpdate): List<ItemFromUpdate> {
        val baseVideoUrl = extractBaseVideoUrl(podcast.url)

        val result = rc
            .get()
            .uri { it
                .path(graphqlEndpoints)
                .queryParam("id", searchQueryId)
                .queryParam("variables", graphQlQuery(podcast.url))
                .build()
            }
            .retrieve()
            .body<TF1GraphQLResult>()
            ?: return emptyList()

        return result.data.programBySlug
            .videos.items
            .map { toItem(it, baseVideoUrl) }
    }

    private fun toItem(video: TF1GraphQLResult.Video, baseVideoUrl: String): ItemFromUpdate {
        val cover = video.bestCover()
            ?.let(image::fetchCoverUpdateInformation)

        return ItemFromUpdate(
            title = video.decoration.label,
            description = video.decoration.description,
            pubDate = video.date,
            url = URI("$baseVideoUrl/${video.slug}.html"),
            cover = cover,
            mimeType = "video/mp4"
        )
    }

    override fun signatureOf(url: URI): String {
        val page = rc
            .get()
            .uri { it
                .path(graphqlEndpoints)
                .queryParam("id", searchQueryId)
                .queryParam("variables", graphQlQuery(url))
                .build()
            }
            .retrieve()
            .body<TF1GraphQLResult>()
            ?: return ""

        val tf1Items = page.data.programBySlug
            .videos.items
            .ifEmpty { return "" }

        return tf1Items
            .map { it.url }
            .sorted()
            .reduce { t, u -> """$t, $u""" }
            .let(String::toByteArray)
            .let(DigestUtils::md5DigestAsHex)
    }

    private fun graphQlQuery(url: URI): String {
        val query = TF1GraphqlQuery(
                programSlug = extractProgram(url),
                offset = 0,
                limit = 50,
                sort = defaultQuerySort,
                types = extractTypeFromUrl(url.toASCIIString())
        )

        return URLEncoder.encode(om.writeValueAsString(query), StandardCharsets.UTF_8.toString())
    }

    private fun extractBaseVideoUrl(url: URI): String {
        val urlAscii = url.toASCIIString()
        val endsWithSection = allowedTypes.any { urlAscii.endsWith(it) }

        if (endsWithSection) {
            return urlAscii.substringBeforeLast("/")
        }

        if (urlAscii.endsWith("videos"))
            return urlAscii

        return "$urlAscii/videos"
    }

    private fun extractProgram(url: URI): String {
        val path = url.path
        return SLUG_EXTRACTOR.on(path).group(1)
            ?: error("Slug not found in podcast with ${url.toASCIIString()}")
    }

    private fun extractTypeFromUrl(url: String): Set<String>? {
        val end = url.substringAfterLast("/")
        if (end in allowedTypes)
            return setOf(end.uppercase(Locale.getDefault()))

        return null
    }

    override fun type() = Type("MyTF1", "MyTF1")

    override fun compatibility(url: String): Int = when {
        "www.tf1.fr" in url -> 1
        else -> Integer.MAX_VALUE
    }

    companion object {
        private const val graphqlEndpoints = "/graphql/web"
        private val allowedTypes = setOf("replay", "extract", "bonus")
        private const val searchQueryId = "87a97a3"

        private val defaultQuerySort = TF1GraphqlQuery.Sort(type = "DATE", order = "DESC")

        private val SLUG_EXTRACTOR = MatcherExtractor.from("/[^/]+/([^/]+)")
    }

}

// {"programSlug":"plan-c","offset":0,"limit":20,"sort":{"type":"DATE","order":"DESC"},"types":["bonus"]}
private class TF1GraphqlQuery(
        val programSlug: String,
        val offset: Int,
        val limit: Int,
        val sort: Sort,
        val types: Set<String>?
) {
    class Sort(val type: String, val order: String)
}

private class TF1GraphQLResult(
        val data: Data
) {
    class Data(val programBySlug: ProgramBySlug)
    class ProgramBySlug(val videos: Videos)
    class Videos(val total: Int, val items: List<Video>)
    class Video(
            val url: String,
            val slug: String,
            val date: ZonedDateTime,
            val decoration: Decoration,
    ) {
        fun bestCover(): URI? {
            return decoration
                    .images
                    .filter { it.type == "THUMBNAIL" }
                    .flatMap { it.sources }
                    .map { URI(it.url) }
                    .firstOrNull()
        }
    }
    class Decoration(val description: String, val images: Set<Images>, val label: String)
    class Images(val type: String, val sources: List<SrcSet>)
    class SrcSet(val url: String)
}

