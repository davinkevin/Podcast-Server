package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@WebFluxTest(controllers = [TagHandler::class])
@Import(TagRoutingConfig::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class TagHandlerTest(
    @Autowired val rest: WebTestClient
) {

    @MockBean private lateinit var tagService: TagService

    @Nested
    @DisplayName("should find tag by id")
    inner class ShouldFindById {

        @Test
        fun `with existing tag`(): Unit = runBlocking {
            /* Given */
            val id = UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf")
            whenever(tagService.findById(id)).thenReturn(Tag(id, "foo"))

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
        fun `with no result`(): Unit = runBlocking {
            /* Given */
            val id = UUID.fromString("fdd3e040-5357-48c6-a31b-da3657ab7adf")
            whenever(tagService.findById(id)).thenReturn(null)

            /* When */
            rest
                    .get()
                    .uri("/api/v1/tags/$id")
                    .exchange()
                    /* Then */
                    .expectStatus().isNotFound
        }

    }

    @Nested
    @DisplayName("should find by name containing")
    inner class ShouldFindByNameContaining {

        @Test
        fun `foo which returns many elements`(): Unit = runBlocking {
            /* Given */
            val t1 = Tag(UUID.randomUUID(), "foo")
            val t2 = Tag(UUID.randomUUID(), "foo2")
            val t3 = Tag(UUID.randomUUID(), "foo3")
            whenever(tagService.findByNameLike("foo")).thenReturn(flowOf(t1, t2, t3))

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
            whenever(tagService.findByNameLike("bar")).thenReturn(flowOf(t1))

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
            whenever(tagService.findByNameLike("tech")).thenReturn(emptyFlow())

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
            whenever(tagService.findByNameLike("")).thenReturn(flowOf(t1, t2, t3))

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
