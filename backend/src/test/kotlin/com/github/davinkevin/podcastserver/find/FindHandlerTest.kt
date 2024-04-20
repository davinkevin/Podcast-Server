package com.github.davinkevin.podcastserver.find

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.kotlin.core.publisher.toMono
import java.net.URI

@WebMvcTest(controllers = [FindHandler::class])
@Import(FindRoutingConfig::class)
class FindHandlerTest(
    @Autowired val rest: WebTestClient
) {

    @MockBean
    private lateinit var finder: FindService

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
                    .bodyValue(url.toASCIIString())
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo(""" {
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
}
