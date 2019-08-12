package com.github.davinkevin.podcastserver.find

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.github.davinkevin.podcastserver.service.CoverInformation
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.toMono
import java.net.URI

@WebFluxTest(controllers = [FindHandler::class])
class FindHandlerTest {

    @Autowired lateinit var rest: WebTestClient
    @Autowired lateinit var finder: FindService

    @Nested
    @DisplayName("should find")
    inner class ShouldFind {

        @Test
        fun `with success`() {
            /* Given */
            val url = URI("http://foo.bar.com/")
            val podcast = FindPodcastInformation(
                    title = "aTitle",
                    url = url,
                    description = "",
                    type = "aType",
                    cover = FindCoverInformation(100, 100, URI("http://foo.bar.com/img.png"))
            )
            whenever(finder.find(url)).thenReturn(podcast.toMono())

            /* When */
            rest
                    .post()
                    .uri("/api/v1/podcasts/find")
                    .syncBody(url.toASCIIString())
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson { isEqualTo(""" {
                        "title":"aTitle",
                        "description":"",
                        "url":"http://foo.bar.com/",
                        "cover":{
                            "height":100,
                            "width":100,
                            "url":"http://foo.bar.com/img.png"
                        },
                        "type":"aType"
                    }""")
                    }
        }

    }

    @TestConfiguration
    @Import(FindRoutingConfig::class)
    class LocalConfiguration {
        @Bean fun findService() = mock<FindService>()
    }

}
