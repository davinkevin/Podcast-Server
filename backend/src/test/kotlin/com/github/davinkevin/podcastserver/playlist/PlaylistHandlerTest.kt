package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
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
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.util.*

/**
 * Created by kevin on 2019-07-06
 */
@WebFluxTest(controllers = [PlaylistHandler::class])
@Import(PlaylistRoutingConfig::class, PlaylistHandler::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class PlaylistHandlerTest (
    @Autowired val rest: WebTestClient,
    @Autowired val service: PlaylistService
) {

    @Nested
    @DisplayName("should find all")
    inner class ShouldFindAll {

        @Test
        fun `with no watch list`() {
            /* Given */
            whenever(service.findAll()).thenReturn(Flux.empty())
            /* When */
            rest
                    .get()
                    .uri("/api/v1/playlists")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{ "content":[] }""")
                    }
        }

        @Test
        fun `with watch lists in results`() {
            /* Given */
            whenever(service.findAll()).thenReturn(Flux.just(
                    Playlist(UUID.fromString("05621536-b211-4736-a1ed-94d7ad494fe0"), "first"),
                    Playlist(UUID.fromString("6e15b195-7a1f-43e8-bc06-bf88b7f865f8"), "second"),
                    Playlist(UUID.fromString("37d09949-6ae0-4b8b-8cc9-79ffd541e51b"), "third")
            ))

            /* When */
            rest
                    .get()
                    .uri("/api/v1/playlists")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                           "content":[
                              { "id":"05621536-b211-4736-a1ed-94d7ad494fe0", "name":"first" },
                              { "id":"6e15b195-7a1f-43e8-bc06-bf88b7f865f8", "name":"second" },
                              { "id":"37d09949-6ae0-4b8b-8cc9-79ffd541e51b", "name":"third" }
                           ]
                        }""")
                    }
        }

    }

    @Nested
    @DisplayName("should find by id")
    inner class ShouldFindById {

        @Test
        fun `with no items`() {
            /* Given */
            val playlist = PlaylistWithItems(id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"), name = "foo", items = emptyList())
            whenever(service.findById(playlist.id)).thenReturn(playlist.toMono())
            /* When */
            rest
                    .get()
                    .uri("/api/v1/playlists/{id}", playlist.id)
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                            "id":"9706ba78-2df2-4b37-a573-04367dc6f0ea",
                            "name":"foo",
                            "items":[]
                        }""")
                    }
        }

        @Test
        fun `with 1 item`() {
            /* Given */
            val playlist = PlaylistWithItems(
                    id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"),
                    name = "foo",
                    items = listOf(
                            PlaylistWithItems.Item(
                                    id = UUID.fromString("c42d2a59-46e6-4c1d-b0fb-2b47d389b370"),
                                    title = "a title",
                                    description = "a desc",
                                    mimeType = "audio/mp3",
                                    fileName = "file.mp3",
                                    podcast = PlaylistWithItems.Item.Podcast(
                                            id = UUID.fromString("3ba6411c-8fb9-4e24-afb1-adbad9a023e0"),
                                            title = "a podcast"
                                    ),
                                    cover = PlaylistWithItems.Item.Cover(
                                            id = UUID.fromString("0882344b-fcaf-4332-9ab8-47e78921f929"),
                                            width = 123,
                                            height = 456,
                                            url = URI("https://foo.com/bar/podcast/image.png")
                                    )
                            )
                    )
            )
            whenever(service.findById(playlist.id)).thenReturn(playlist.toMono())
            /* When */
            rest
                    .get()
                    .uri("/api/v1/playlists/{id}", playlist.id)
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                            "id":"9706ba78-2df2-4b37-a573-04367dc6f0ea",
                            "name":"foo",
                            "items":[
                               {
                                  "cover":{
                                     "height":456,
                                     "id":"0882344b-fcaf-4332-9ab8-47e78921f929",
                                     "url":"/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/cover.png",
                                     "width":123
                                  },
                                  "description":"a desc",
                                  "id":"c42d2a59-46e6-4c1d-b0fb-2b47d389b370",
                                  "mimeType":"audio/mp3",
                                  "podcast":{
                                     "id":"3ba6411c-8fb9-4e24-afb1-adbad9a023e0",
                                     "title":"a podcast"
                                  },
                                  "proxyURL":"/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/a_title.mp3",
                                  "title":"a title"
                               }
                            ]
                        }""")
                    }
        }

        @Test
        fun `with 2 item`() {
            /* Given */
            val playlist = PlaylistWithItems(
                    id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"),
                    name = "foo",
                    items = listOf(
                            PlaylistWithItems.Item(
                                    id = UUID.fromString("c42d2a59-46e6-4c1d-b0fb-2b47d389b370"),
                                    title = "a title",
                                    description = "a desc",
                                    mimeType = "audio/mp3",
                                    fileName = "file.mp3",
                                    podcast = PlaylistWithItems.Item.Podcast(
                                            id = UUID.fromString("3ba6411c-8fb9-4e24-afb1-adbad9a023e0"),
                                            title = "a podcast"
                                    ),
                                    cover = PlaylistWithItems.Item.Cover(
                                            id = UUID.fromString("0882344b-fcaf-4332-9ab8-47e78921f929"),
                                            width = 123,
                                            height = 456,
                                            url = URI("https://foo.com/bar/podcast/image.png")
                                    )
                            ),
                            PlaylistWithItems.Item(
                                    id = UUID.fromString("4b48996c-686f-4339-b94e-f9595094f2ea"),
                                    title = "2 a title",
                                    description = "a desc",
                                    mimeType = "audio/mp3",
                                    fileName = "file2.mp3",
                                    podcast = PlaylistWithItems.Item.Podcast(
                                            id = UUID.fromString("35d04720-1bc3-476b-b7b0-494a15adf45e"),
                                            title = "2 a podcast"
                                    ),
                                    cover = PlaylistWithItems.Item.Cover(
                                            id = UUID.fromString("f1b81640-ff21-423b-9cc4-8c256e412e14"),
                                            width = 123,
                                            height = 456,
                                            url = URI("https://foo.com/bar/podcast/2/image.png")
                                    )
                            )
                    )
            )
            whenever(service.findById(playlist.id)).thenReturn(playlist.toMono())
            /* When */
            rest
                    .get()
                    .uri("/api/v1/playlists/{id}", playlist.id)
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                            "id":"9706ba78-2df2-4b37-a573-04367dc6f0ea",
                            "name":"foo",
                            "items":[
                               {
                                  "cover":{
                                     "height":456,
                                     "id":"0882344b-fcaf-4332-9ab8-47e78921f929",
                                     "url":"/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/cover.png",
                                     "width":123
                                  },
                                  "description":"a desc",
                                  "id":"c42d2a59-46e6-4c1d-b0fb-2b47d389b370",
                                  "mimeType":"audio/mp3",
                                  "podcast":{
                                     "id":"3ba6411c-8fb9-4e24-afb1-adbad9a023e0",
                                     "title":"a podcast"
                                  },
                                  "proxyURL":"/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/a_title.mp3",
                                  "title":"a title"
                               },
                               {
                                  "cover":{
                                     "height":456,
                                     "id":"f1b81640-ff21-423b-9cc4-8c256e412e14",
                                     "url":"/api/v1/podcasts/35d04720-1bc3-476b-b7b0-494a15adf45e/items/4b48996c-686f-4339-b94e-f9595094f2ea/cover.png",
                                     "width":123
                                  },
                                  "description":"a desc",
                                  "id":"4b48996c-686f-4339-b94e-f9595094f2ea",
                                  "mimeType":"audio/mp3",
                                  "podcast":{
                                     "id":"35d04720-1bc3-476b-b7b0-494a15adf45e",
                                     "title":"2 a podcast"
                                  },
                                  "proxyURL":"/api/v1/podcasts/35d04720-1bc3-476b-b7b0-494a15adf45e/items/4b48996c-686f-4339-b94e-f9595094f2ea/2_a_title.mp3",
                                  "title":"2 a title"
                               }
                            ]
                        }""")
                    }
        }


    }

    @TestConfiguration
    class LocalTestConfiguration {
        @Bean fun service() = mock<PlaylistService>()
    }
}
