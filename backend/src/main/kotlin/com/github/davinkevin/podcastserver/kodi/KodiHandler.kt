package com.github.davinkevin.podcastserver.kodi

import com.github.davinkevin.podcastserver.extension.serverRequest.extractHost
import org.slf4j.LoggerFactory
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
            .joinToString(separator = "\n") {
                """
                    <tr>
                        <td><a href="${it.title.encode()}/?podcastId=${it.id}">${it.title}/</a></td><td align="right">  - </td><td align="right">  - </td>
                    </tr>
                """.trimIndent()
            }

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
            .joinToString(separator = "\n") {
                """
                    <tr>
                        <td><a href="${it.title.encode()}.${it.extension()}?podcastId=${podcastId}&itemId=${it.id}">${it.title}.${it.extension()}</a></td>
                        <td align="right">${it.formattedDate()}</td>
                        <td align="right">${it.length}B</td>
                    </tr>
                """
                    .replaceIndent("        ")
            }

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

        val itemTitle = r.pathVariable("itemTitle")

        return ServerResponse.seeOther(
            URI.create("${host}api/v1/podcasts/${podcastId}/items/${itemId}/${itemTitle.encode()}")
        ).build()
    }

}

private fun String.encode() = URLEncoder.encode(this, "UTF-8").replace("+", "%20")
private fun Item.extension() = this.fileName?.extension ?: mimeType.substringAfter("/")
private fun Item.formattedDate() = this.pubDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))