package com.github.davinkevin.podcastserver.controller.api

import com.github.davinkevin.podcastserver.business.ItemBusiness
import com.github.davinkevin.podcastserver.service.UrlService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.util.*

//@Controller
@RequestMapping("/api/podcasts/{idPodcast}/items")
class ItemRedirection(private val itemBusiness: ItemBusiness) {

    internal var log = LoggerFactory.getLogger(ItemRedirection::class.java)

    @GetMapping(value = ["{id}/{file}"])
    fun file(@PathVariable id: UUID, exchange: ServerWebExchange): Mono<Void> {
        val item = itemBusiness.findOne(id)

        val redirect = if (item.isDownloaded) {
            UriComponentsBuilder.fromUri(UrlService.getDomainFromRequest(exchange))
                    .pathSegment("data", item.podcast!!.title, item.fileName)
                    .build().toUri()
                    .toASCIIString()
        } else {
            item.url
        }

        log.info("redirection to {}", redirect)
        exchange.response.statusCode = HttpStatus.SEE_OTHER
        exchange.response.headers.add(HttpHeaders.LOCATION, redirect)

        return exchange.response.setComplete()
    }
}