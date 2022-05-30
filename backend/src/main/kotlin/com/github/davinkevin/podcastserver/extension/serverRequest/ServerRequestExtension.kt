package com.github.davinkevin.podcastserver.extension.serverRequest

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.util.UriBuilder
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Created by kevin on 2019-02-12
 */

val EMPTY_PORTS = setOf(80, 443)

fun ServerRequest.extractHost(): URI {
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

fun ServerRequest.normalizedURI(): URI {
    return UriComponentsBuilder.fromUri(extractHost())
        .path(path())
        .queryParams(queryParams())
        .build(true)
        .toUri()
}
