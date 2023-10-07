package com.github.davinkevin.podcastserver.service.image

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.update.updaters.ItemFromUpdate
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.InputStream
import java.net.URI
import javax.imageio.ImageIO

class ImageService (private val wcb: WebClient.Builder ) {

    private val log = LoggerFactory.getLogger(ImageService::class.java)

    fun fetchCoverInformation(url: URI): Mono<CoverInformation> {
        log.debug("fetch $url")
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
val defaultCoverInformation = CoverInformation(
    height = 600,
    url = URI.create("https://placehold.co/600x600?text=No+Cover"),
    width = 600,
)

private fun InputStream.toBufferedImage() = ImageIO.read(ImageIO.createImageInputStream(this))!!
fun CoverInformation.toCoverForCreation() = CoverForCreation(
    height = this.height,
    width = this.width,
    url = this.url
)
