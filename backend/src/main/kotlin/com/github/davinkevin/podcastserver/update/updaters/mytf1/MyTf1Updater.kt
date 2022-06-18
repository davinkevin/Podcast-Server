package com.github.davinkevin.podcastserver.update.updaters.mytf1

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.Type
import com.github.davinkevin.podcastserver.update.updaters.Updater
import com.github.davinkevin.podcastserver.update.fetchCoverUpdateInformationOrOption
import com.github.davinkevin.podcastserver.utils.MatcherExtractor
import org.springframework.util.DigestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import com.github.davinkevin.podcastserver.service.image.ImageService

/**
 * Created by kevin on 11/03/2020
 */
class MyTf1Updater(
        private val wc: WebClient,
        private val om: ObjectMapper,
        private val image: ImageService
): Updater {

    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> {
        val baseVideoUrl = extractBaseVideoUrl(podcast.url)
        return wc
                .get()
                .uri { it
                        .path(graphqlEndpoints)
                        .queryParam("id", searchQueryId)
                        .queryParam("variables", graphQlQuery(podcast.url))
                        .build()
                }
                .retrieve()
                .bodyToMono<TF1GraphQLResult>()
                .flatMapIterable { it.data.programBySlug.videos.items }
                .flatMap { toItem(it, baseVideoUrl) }

    }

    private fun toItem(video: TF1GraphQLResult.Video, baseVideoUrl: String): Mono<ItemFromUpdate> {
        return image
                .fetchCoverUpdateInformationOrOption(video.bestCover())
                .map {
                    ItemFromUpdate(
                            title = video.title,
                            description = video.decoration.description,
                            pubDate = video.date,
                            url = URI("$baseVideoUrl/${video.slug}.html"),
                            cover = it.orNull(),
                            mimeType = "video/mp4"
                    )
                }
    }

    override fun signatureOf(url: URI): Mono<String> {
        return wc
                .get()
                .uri { it
                        .path(graphqlEndpoints)
                        .queryParam("id", searchQueryId)
                        .queryParam("variables", graphQlQuery(url))
                        .build()
                }
                .retrieve()
                .bodyToMono<TF1GraphQLResult>()
                .flatMapIterable { it.data.programBySlug.videos.items }
                .map { it.streamId }
                .sort()
                .reduce { t, u -> """$t, $u""" }
                .map { DigestUtils.md5DigestAsHex(it.toByteArray()) }
                .switchIfEmpty("".toMono())
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
        return SLUG_EXTRACTOR.on(path).group(1) ?: error("Slug not found in podcast with ${url.toASCIIString()}")
    }

    private fun extractTypeFromUrl(url: String): Set<String>? {
        val end = url.substringAfterLast("/")
        if (end in allowedTypes)
            return setOf(end)

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
        private const val searchQueryId = "6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91"

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
            val streamId: String,
            val slug: String,
            val title: String,
            val date: ZonedDateTime,
            val decoration: Decoration
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
    class Decoration(val description: String, val images: Set<Images>)
    class Images(val type: String, val sources: List<SrcSet>)
    class SrcSet(val url: String)
}

