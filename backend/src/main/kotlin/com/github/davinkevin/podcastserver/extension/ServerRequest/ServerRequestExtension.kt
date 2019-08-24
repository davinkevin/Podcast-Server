package com.github.davinkevin.podcastserver.extension.ServerRequest

import arrow.core.orElse
import arrow.syntax.collections.firstOption
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.server.ServerRequest
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

    return URI("$proto://$host$portAsString/")
}
