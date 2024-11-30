package com.github.davinkevin.podcastserver.kodi

import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import org.springframework.http.MediaType
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.paramOrNull
import java.net.URI
import java.net.URLEncoder
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.extension

class KodiHandler(
    private val kodi: KodiRepository
) {

    val header = """
        <!DOCTYPE html>
        <html>
            <head>
                <meta charset="utf-8"/>
            </head>
            <body>
                <table>
                    <thead>
                    <tr>
                        <th><a>Name</a></th><th><a>Last modified</a></th><th><a>Size</a></th>
                    </tr>
                    </thead>
                    <tbody>
    """.trimIndent()
    val footer = """
                    </tbody>
                </table>
            </body>
        </html>
    """.trimIndent()

    fun podcasts(@Suppress("UNUSED_PARAMETER") r: ServerRequest): ServerResponse {
        val response = kodi
            .podcasts()
            .map(::PodcastHTML)
            .joinToString(separator = "\n") { it.toTableRow() }

        return ServerResponse.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(header + "\n" + response + "\n" + footer)
    }

    fun items(r: ServerRequest): ServerResponse {
        val podcastId = r
            .paramOrNull("podcastId")
            ?.let(UUID::fromString)
            ?: error("podcastId query param not provided")

        val response = kodi
            .items(podcastId = podcastId)
            .map { ItemHTML(it, podcastId) }
            .joinToString(separator = "\n") { it.toTableRow().replaceIndent("        ") }

        return ServerResponse.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(header + "\n" + response + "\n" + footer)
    }

    fun item(r: ServerRequest): ServerResponse {
        val host = r.extractHost()

        val itemId = r.paramOrNull("itemId")
            ?.let(UUID::fromString)
            ?: error("itemId query param not provided")

        val podcastId = r
            .paramOrNull("podcastId")
            ?.let(UUID::fromString)
            ?: error("podcastId query param not provided")

        val itemTitle = URLEncoder.encode(r.pathVariable("itemTitle"), "UTF-8")

        return ServerResponse.seeOther(
            URI.create("${host}api/v1/podcasts/${podcastId}/items/${itemId}/${itemTitle}")
        ).build()
    }
}

class PodcastHTML(private val podacst: Podcast) {

    val title = URLEncoder.encode(podacst.title, "UTF-8")
        .replace("+", "%20")

    fun toTableRow() =
        """
            <tr>
                <td><a href="${title}/?podcastId=${podacst.id}">${podacst.title}/</a></td>
                <td align="right"> - </td>
                <td align="right"> - </td>
            </tr>
        """.trimIndent()

}

class ItemHTML(private val item: Item, private val podcastId: UUID) {

    private val title = item.title.replace(KEEP_STANDARD_CHARACTERS, "")

    private fun href(): String {
        val encodedTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
        return "${encodedTitle}.${extension()}?podcastId=${podcastId}&itemId=${item.id}"
    }
    private fun extension() = item.fileName?.extension ?: item.mimeType.substringAfter("/")

    fun toTableRow(): String =
        """
            <tr>
                <td><a href="${href()}">${title}.${extension()}</a></td>
                <td align="right">${item.pubDate.format(KODI_DATE_FORMAT)}</td>
                <td align="right">${item.length}B</td>
            </tr>
        """

    companion object {
        private val KEEP_STANDARD_CHARACTERS = "[^\\p{L}\\p{N}\\p{P}\\p{Z}]".toRegex()
        private val KODI_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}