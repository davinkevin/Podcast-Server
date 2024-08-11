package com.github.davinkevin.podcastserver.service.image

import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.io.InputStream
import java.net.URI
import javax.imageio.ImageIO

class ImageService (private val rcb: RestClient.Builder ) {

    private val log = LoggerFactory.getLogger(ImageService::class.java)

    fun fetchCoverInformation(url: URI): CoverInformation? {
        log.debug("fetch $url")

        val content = rcb
            .clone()
            .baseUrl(url.toASCIIString()).build()
            .get()
            .accept(MediaType.ALL, MediaType.APPLICATION_OCTET_STREAM)
            .runCatching {
                retrieve().body<ByteArrayResource>()
            }
            .onFailure { log.error("Error during download of {}", url, it) }
            .getOrNull()
            ?: return null

      val image = content.inputStream.toBufferedImage()

      return CoverInformation(width = image.width, height = image.height, url = url)
    }
}

data class CoverInformation(val width: Int, val height: Int, val url: URI)

private fun InputStream.toBufferedImage() = ImageIO.read(ImageIO.createImageInputStream(this))!!
