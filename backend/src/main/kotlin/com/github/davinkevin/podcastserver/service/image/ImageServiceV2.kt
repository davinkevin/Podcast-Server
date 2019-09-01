package com.github.davinkevin.podcastserver.service.image

import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.InputStream
import java.net.URI
import javax.imageio.ImageIO

class ImageServiceV2 ( private val wcb: WebClient.Builder ) {

    private val log = LoggerFactory.getLogger(ImageServiceV2::class.java)

    fun fetchCoverInformation(url: URI): Mono<CoverInformation> {
        log.info("fetch $url")
        return wcb
                .clone()
                .baseUrl(url.toASCIIString()).build()
                .get()
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(ByteArrayResource::class.java)
                .map { it.inputStream.toBufferedImage() }
                .map { CoverInformation(width = it.width, height = it.height, url = url) }
                .onErrorResume { Mono.empty() }
    }
}

data class CoverInformation(val width: Int, val height: Int, val url: URI)

private fun InputStream.toBufferedImage() = ImageIO.read(ImageIO.createImageInputStream(this))!!
