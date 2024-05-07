package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.github.davinkevin.podcastserver.extension.mockmvc.MockMvcRestExceptionConfiguration
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@WebMvcTest(controllers = [TagHandler::class])
@Import(TagRoutingConfig::class, MockMvcRestExceptionConfiguration::class)
class TagHandlerTest(
    @Autowired val rest: WebTestClient
) {

    @MockBean private lateinit var tagService: TagService

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
                    .expectStatus()
                    .isNotFound
        }
    }

    @Nested
    @DisplayName("should find by name containing")
    inner class ShouldFindByNameContaining {

        @Test
        fun `foo which returns many elements`() {
            /* Given */
            val t1 = Tag(UUID.randomUUID(), "foo")
            val t2 = Tag(UUID.randomUUID(), "foo2")
            val t3 = Tag(UUID.randomUUID(), "foo3")
            whenever(tagService.findByNameLike("foo")).thenReturn(Flux.just(t1, t2, t3))

            /* When */
            rest
                    .get()
                    .uri { it.path("/api/v1/tags/search").queryParam("name", "foo").build() }
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                            "content": [ {
                                    "id": "${t1.id}",
                                    "name": "foo"
                                }, {
                                    "id": "${t2.id}",
                                    "name": "foo2"
                                }, {
                                    "id": "${t3.id}",
                                    "name": "foo3"
                                } ]
                        }
                        """)
                    }
        }

        @Test
        fun `bar which returns one element`() {
            /* Given */
            val t1 = Tag(UUID.randomUUID(), "bar")
            whenever(tagService.findByNameLike("bar")).thenReturn(Flux.just(t1))

            /* When */
            rest
                    .get()
                    .uri { it.path("/api/v1/tags/search").queryParam("name", "bar").build() }
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                            "content": [ {
                                    "id": "${t1.id}",
                                    "name": "bar"
                                } ]
                        }
                        """)
                    }
        }

        @Test
        fun `tech which returns no element`() {
            /* Given */
            whenever(tagService.findByNameLike("tech")).thenReturn(Flux.empty())

            /* When */
            rest
                    .get()
                    .uri { it.path("/api/v1/tags/search").queryParam("name", "tech").build() }
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{ "content": [  ] } """)
                    }
        }

        @Test
        fun `no word as parameter`() {
            /* Given */
            val t1 = Tag(UUID.randomUUID(), "foo")
            val t2 = Tag(UUID.randomUUID(), "foo2")
            val t3 = Tag(UUID.randomUUID(), "foo3")
            whenever(tagService.findByNameLike("")).thenReturn(Flux.just(t1, t2, t3))

            /* When */
            rest
                    .get()
                    .uri { it.path("/api/v1/tags/search").queryParam("name", "").build() }
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                            "content": [ {
                                    "id": "${t1.id}",
                                    "name": "foo"
                                }, {
                                    "id": "${t2.id}",
                                    "name": "foo2"
                                }, {
                                    "id": "${t3.id}",
                                    "name": "foo3"
                                } ]
                        }
                        """)
                    }
        }
    }
}
