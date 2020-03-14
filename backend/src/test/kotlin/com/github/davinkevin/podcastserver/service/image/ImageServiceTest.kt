package com.github.davinkevin.podcastserver.service.image

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.test.StepVerifier
import java.net.URI
import com.github.davinkevin.podcastserver.service.image.ImageService

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
        StepVerifier.create(imageService.fetchCoverInformation(url))
                /* Then */
                .expectSubscription()
                .assertNext { cover ->
                    assertThat(cover.width).isEqualTo(256)
                    assertThat(cover.height).isEqualTo(300)
                    assertThat(cover.url).isEqualTo(url)
                }
                .verifyComplete()
    }

    @Test
    fun `should return empty if file not found`(backend: WireMockServer) {
        /* Given */
        val url = URI("http://localhost:5555/img/image.png")
        backend.stubFor(get("/img/image.png").willReturn(notFound()))

        /* When */
        StepVerifier.create(imageService.fetchCoverInformation(url))
                /* Then */
                .expectSubscription()
                .verifyComplete()
    }

    @TestConfiguration
    @Import(ImageServiceConfig::class, WebClientAutoConfiguration::class, WebClientConfig::class)
    class LocalTestConfiguration

}
