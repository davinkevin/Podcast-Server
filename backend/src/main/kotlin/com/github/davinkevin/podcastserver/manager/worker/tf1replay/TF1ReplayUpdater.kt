package com.github.davinkevin.podcastserver.manager.worker.tf1replay

import arrow.core.Option
import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.manager.worker.*
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.MatcherExtractor.Companion.from
import com.github.davinkevin.podcastserver.utils.k
import lan.dk.podcastserver.service.JsonService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime

/**
 * Created by kevin on 20/07/2016
 */
@Component
class TF1ReplayUpdater(val signatureService: SignatureService, val om: ObjectMapper, val jsonService: JsonService, val coverService: ImageService) : Updater {

    private val log = LoggerFactory.getLogger(TF1ReplayUpdater::class.java)!!

    override fun blockingFindItems(podcast: PodcastToUpdate): Set<ItemFromUpdate> {
        val baseVideoUrl = extractBaseVideoUrl(podcast.url)
        val url = generateQueryUrl(podcast.url)

        return jsonService
                .parseUrl(url).k()
                .map { JsonService.to("data.programBySlug.videos", TF1VideosConnector::class.java).apply(it) }
                .map { it.items }
                .getOrElse {
                    log.error("No items found for podcast with id ${podcast.id} at ${podcast.url}, the layout may have changed")
                    setOf()
                }
                .map { toItem(it, baseVideoUrl) }
                .toSet()
    }

    private fun extractBaseVideoUrl(url: URI): String {
        val urlAscii = url.toASCIIString()
        val endsWithSection = allowedTypes
                .asSequence()
                .any { urlAscii.endsWith(it) }

        if (endsWithSection) {
            return urlAscii.substringBeforeLast("/")
        }

        if (urlAscii.endsWith("videos"))
            return urlAscii

        return "$urlAscii/videos"
    }

    private fun extractProgram(url: URI): String {
        val path = url.path

        return SLUG_EXTRACTOR.on(path)
                .group(1)
                .getOrElse { throw RuntimeException("Slug not found in podcast with ${url.toASCIIString()}") }
    }

    private fun extractTypeFromUrl(url: String): Set<String>? {
        val end = url.substringAfterLast("/")
        if (end in allowedTypes)
            return setOf(end)

        return null
    }

    private fun generateQueryUrl(url: URI): String {
        val types = extractTypeFromUrl(url.toASCIIString())
        val program = extractProgram(url)

        val query = TF1GraphqlQuery(programSlug = program, offset = 0, limit = 50, sort = defaultQuerySort, types = types)
        val queryEncoded = URLEncoder.encode(om.writeValueAsString(query), StandardCharsets.UTF_8.toString())
        return "$endpoint?id=$searchQueryId&variables=$queryEncoded"
    }

    private fun toItem(video: TF1Video, baseVideoUrl: String ): ItemFromUpdate = ItemFromUpdate(
        title = video.title,
        description = video.decoration.description,
        pubDate = video.date,
        url = URI("$baseVideoUrl/${video.slug}.html"),
        cover = video.bestCover().flatMap { Option.fromNullable(coverService.fetchCoverInformation(it)) }.orNull()?.toCoverFromUpdate()
    )

    override fun blockingSignatureOf(url: URI): String {
        val podcastUrl = generateQueryUrl(url)

        return jsonService
                .parseUrl(podcastUrl).k()
                .map { JsonService.extract<List<String>>("data.programBySlug.videos.items[*].streamId").apply(it) }
                .map { it.sorted().joinToString() }
                .map { signatureService.fromText(it) }
                .getOrElse { throw RuntimeException("Error during signature of podcast with url ${url.toASCIIString()}") }
    }

    override fun type() = Type("TF1Replay", "TF1 Replay")

    override fun compatibility(url: String?) =
            if ("www.tf1.fr" in (url ?: "")) 1
            else Integer.MAX_VALUE

    companion object {
        private const val endpoint = "https://www.tf1.fr/graphql/web"
        private val allowedTypes = setOf("replay", "extract", "bonus")
        private const val searchQueryId = "6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91"

        private val defaultQuerySort = TF1GraphqlQuerySort(type = "DATE", order = "DESC")

        private val SLUG_EXTRACTOR = from("/[^/]+/([^/]+)")
    }
}

// {"programSlug":"plan-c","offset":0,"limit":20,"sort":{"type":"DATE","order":"DESC"},"types":["bonus"]}
@JsonIgnoreProperties(ignoreUnknown = true) private class TF1GraphqlQuery(val programSlug: String, val offset: Int, val limit: Int, val sort: TF1GraphqlQuerySort, val types: Set<String>?)
@JsonIgnoreProperties(ignoreUnknown = true) private class TF1GraphqlQuerySort(val type: String, val order: String)

@JsonIgnoreProperties(ignoreUnknown = true) private class TF1VideosConnector(val total: Int, val items: Set<TF1Video>)
@JsonIgnoreProperties(ignoreUnknown = true) private class TF1Video(val slug: String, val title: String, val date: ZonedDateTime, val decoration: TF1Decoration) {
    fun bestCover(): Option<String> = decoration
            .images
            .filter { it.type == "THUMBNAIL" }
            .flatMap { it.sources }
            .firstOption()
            .map { it.url }
}
@JsonIgnoreProperties(ignoreUnknown = true) private class TF1Decoration(val description: String, val images: Set<TF1Images>)

@JsonIgnoreProperties(ignoreUnknown = true) private class TF1Images(val type: String, val sources: List<TF1SrcSet>)
@JsonIgnoreProperties(ignoreUnknown = true) private class TF1SrcSet(val url: String)
