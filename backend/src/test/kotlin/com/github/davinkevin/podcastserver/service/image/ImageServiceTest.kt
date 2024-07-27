package com.github.davinkevin.podcastserver.service.image

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI

@ExtendWith(SpringExtension::class, MockServer::class)
class ImageServiceTest(
        @Autowired val imageService: ImageService
) {

    @Test
    fun `should serve cover information`(backend: WireMockServer) {
        /* Given */
        val url = URI("http://localhost:5555/img/image.png")
        backend.stubFor(get("/img/image.png")
                .willReturn(aResponse().withBodyFile("img/image.png"))
        )

        /* When */
        val cover = imageService.fetchCoverInformation(url)

        /* Then */
        assertThat(cover).isNotNull()
        assertAll {
            assertThat(cover?.width).isEqualTo(256)
            assertThat(cover?.height).isEqualTo(300)
            assertThat(cover?.url).isEqualTo(url)
        }
    }

    @Test
    fun `should return empty if file not found`(backend: WireMockServer) {
        /* Given */
        val url = URI("http://localhost:5555/img/image.png")
        backend.stubFor(get("/img/image.png").willReturn(notFound()))

        /* When */
        val cover = imageService.fetchCoverInformation(url)

        /* Then */
        assertThat(cover).isNull()
    }

    @Test
    fun `should return empty if file is empty`(backend: WireMockServer) {
        /* Given */
        val url = URI("http://localhost:5555/img/image.png")
        backend.stubFor(get("/img/image.png").willReturn(ok()))

        /* When */
        val cover = imageService.fetchCoverInformation(url)

        /* Then */
        assertThat(cover).isNull()
    }

    @TestConfiguration
    @Import(ImageServiceConfig::class, RestClientAutoConfiguration::class)
    class LocalTestConfiguration

}
