package com.github.davinkevin.podcastserver.extension.serverRequest

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.item.ItemPageRequest
import com.github.davinkevin.podcastserver.item.ItemSort
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.paramOrNull
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import org.springframework.web.reactive.function.server.ServerRequest as ReactiveServerRequest

/**
 * Created by kevin on 2019-02-12
 */

val EMPTY_PORTS = setOf(80, 443)

fun ReactiveServerRequest.extractHost(): URI {
    val headers = this.headers()
    val uri = uri()

    val host = headers.header("Host").firstOrNull()
            ?: headers.header("X-Forwarded-Host").firstOrNull()
            ?: uri.host

    val proto = headers.header("X-Forwarded-Proto").firstOrNull()
            ?: uri.scheme

    val port = headers.header("X-Forwarded-Port").map { it.toInt() }.firstOrNull()
            ?: (if (uri.port == -1) null else uri.port)
            ?: if (proto == "https") 443 else 80

    val portAsString = if(port in EMPTY_PORTS) "" else ":$port"

    return URI.create("$proto://$host$portAsString/")
}

fun ReactiveServerRequest.normalizedURI(): URI {
    return UriComponentsBuilder.fromUri(extractHost())
        .path(path())
        .queryParams(queryParams())
        .build(true)
        .toUri()
}

fun ServerRequest.extractHost(): URI {
    val headers = this.headers()
    val uri = uri()

    val host = headers.header("Host").firstOrNull()
        ?: headers.header("X-Forwarded-Host").firstOrNull()
        ?: uri.host

    val proto = headers.header("X-Forwarded-Proto").firstOrNull()
        ?: uri.scheme

    val port = headers.header("X-Forwarded-Port").firstOrNull()?.toIntOrNull()
        ?: (if (uri.port != -1) uri.port else null)
        ?: if (proto == "https") 443 else 80

    val portAsString = if(port in EMPTY_PORTS) "" else ":$port"

    return URI.create("$proto://$host$portAsString/")
}

fun ServerRequest.toPageRequest(): ItemPageRequest {
    val page = paramOrNull("page")?.toInt() ?: 0
    val size  = paramOrNull("size")?.toInt() ?: 12
    val (field, direction) = paramOrNull("sort")?.split(",") ?: listOf("pubDate", "DESC")

    return ItemPageRequest(page, size, ItemSort(direction, field))
}

fun ServerRequest.extractTags(): List<String> {
    val tags = paramOrNull("tags")

    if (tags.isNullOrEmpty()) return emptyList()

    return tags
        .split(",")
        .filter { it.isNotEmpty() }
}

fun ServerRequest.extractStatus(): List<Status> {
    val statuses = paramOrNull("status")

    if (statuses.isNullOrEmpty()) return emptyList()

    return statuses
        .split(",")
        .filter { it.isNotEmpty() }
        .map(Status::of)
}

fun ServerRequest.extractQuery(): String = paramOrNull("q") ?: ""
