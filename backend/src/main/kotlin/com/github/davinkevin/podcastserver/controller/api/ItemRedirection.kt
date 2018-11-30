package com.github.davinkevin.podcastserver.controller.api

import com.github.davinkevin.podcastserver.business.ItemBusiness
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.util.*

@Controller
@RequestMapping("/api/podcasts/{idPodcast}/items")
class ItemRedirection(val itemBusiness: ItemBusiness) {

    internal var log = org.slf4j.LoggerFactory.getLogger(ItemRedirection::class.java)

    @GetMapping(value = ["{id}/{file}"])
    fun file(@PathVariable id: UUID, exchange: ServerWebExchange): Mono<Void> {
        val item = itemBusiness.findOne(id)

        val redirect = if (item.isDownloaded) {
            UriComponentsBuilder.fromUri(extractDomainWithSchemeAndPort(exchange))
                    .pathSegment("data", item.podcast!!.title, item.fileName)
                    .build().toUri().toASCIIString()
        } else item.url

        log.info("new location is {}", redirect)
        exchange.response.statusCode = HttpStatus.SEE_OTHER
        exchange.response.headers.add(HttpHeaders.LOCATION, redirect)
        return exchange.response.setComplete()
    }
}

fun extractDomainWithSchemeAndPort(ex: ServerWebExchange): URI {
    val origin = ex.request.headers["origin"]?.firstOrNull()
    if (origin != null) {
        return URI(origin)
    }
    val uri = ex.request.uri
    return URI("${uri.scheme}://${uri.host}:${uri.port}/")
}