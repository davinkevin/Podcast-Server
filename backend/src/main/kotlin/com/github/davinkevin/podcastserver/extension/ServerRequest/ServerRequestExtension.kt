package com.github.davinkevin.podcastserver.extension.ServerRequest

import org.springframework.web.reactive.function.server.ServerRequest
import java.net.URI

/**
 * Created by kevin on 2019-02-12
 */

val EMPTY_PORTS = setOf(80, 443)

fun ServerRequest.extractHost(): URI {
    val origin = this.headers().header("Origin").firstOrNull()
    if (origin != null) {
        return URI(origin)
    }
    val uri = this.uri()
    val port = if(uri.port in EMPTY_PORTS) { "" } else { ":${uri.port}" }
    return URI("${uri.scheme}://${uri.host}$port/")
}
