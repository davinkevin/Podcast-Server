package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.util.*

@WebFluxTest(controllers = [TagHandler::class])
@Import(TagRoutingConfig::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class TagHandlerTest {

    @Autowired lateinit var rest: WebTestClient
    @MockBean lateinit var tagService: TagService

    @Nested
    @DisplayName("should find tag by id")
    inner class ShouldFindById {

        @Test
        fun `with existing tag`() {
            /* Given */
            val id = UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf")
            whenever(tagService.findById(id)).thenReturn(Tag(id, "foo").toMono())

            /* When */
            rest
                    .get()
                    .uri("/api/v1/tags/$id")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                                "id":"fdd3e040-5357-48c6-a31b-da3657ab7adf",
                                "name":"foo"
                            }""")
                    }
        }

        @Test
        fun `with no result`() {
            /* Given */
            val id = UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf")
            whenever(tagService.findById(id)).thenReturn(Mono.empty())

            /* When */
            rest
                    .get()
                    .uri("/api/v1/tags/$id")
                    .exchange()
                    /* Then */
                    .expectStatus().isNotFound
        }

    }
}
