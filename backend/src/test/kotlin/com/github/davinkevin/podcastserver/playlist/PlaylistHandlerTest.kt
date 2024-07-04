package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.github.davinkevin.podcastserver.extension.mockmvc.MockMvcRestExceptionConfiguration
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.io.path.Path

@WebMvcTest(controllers = [PlaylistHandler::class])
@Import(PlaylistRoutingConfig::class, PlaylistHandler::class, MockMvcRestExceptionConfiguration::class)
class PlaylistHandlerTest (
    @Autowired val rest: WebTestClient
) {

    @MockBean private lateinit var service: PlaylistService

    @Nested
    @DisplayName("should save")
    inner class ShouldSave {

        val item = PlaylistWithItems.Item(
            id = UUID.fromString("c42d2a59-46e6-4c1d-b0fb-2b47d389b370"),
            title = "a title",
            description = "a desc",
            mimeType = "audio/mp3",
            fileName = Path("file.mp3"),
            length = 10L,
            pubDate = OffsetDateTime.of(2019, 10, 27, 10, 10, 10, 10, ZoneOffset.UTC),
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
        val playlist = PlaylistWithItems(
            id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"),
            name = "foo",
            items = listOf(item)
        )

        @Test
        fun `with a name`() {
            /* Given */
            val playlist = PlaylistWithItems(id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"), name = "foo", items = emptyList())
            whenever(service.save("foo")).thenReturn(playlist)
            /* When */
            rest
                    .post()
                    .uri("/api/v1/playlists")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""{"name":"${playlist.name}"}""")
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
            whenever(service.save("foo")).thenReturn(playlist)

            /* When */
            rest
                .post()
                .uri("/api/v1/playlists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""{"name":"${playlist.name}"}""")
                .exchange()
                /* Then */
                .expectStatus().isOk
                .expectBody()
                .assertThatJson {
                    isEqualTo("""{
                            "id":"9706ba78-2df2-4b37-a573-04367dc6f0ea",
                            "name":"foo",
                            "items":[{
                                "id": "c42d2a59-46e6-4c1d-b0fb-2b47d389b370",
                                "title": "a title",
                                "proxyURL": "/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/a-title.mp3",
                                "description": "a desc",
                                "mimeType": "audio/mp3",
                                "podcast": {
                                  "id": "3ba6411c-8fb9-4e24-afb1-adbad9a023e0",
                                  "title": "a podcast"
                                },
                                "cover": {
                                  "id": "0882344b-fcaf-4332-9ab8-47e78921f929",
                                  "width": 123,
                                  "height": 456,
                                  "url": "/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/cover.png"
                                }
                              }]
                        }""")
                }
        }

        @Test
        fun `with 1 item without cover extension`() {
            /* Given */
            val playlistWithItemWithoutCoverExtension = playlist.copy(
                items = listOf(
                    item.copy(
                        cover = item.cover.copy(
                            url = URI("https://foo.com/bar/podcast/image")
                        )
                    )
                )
            )
            whenever(service.save("foo"))
                .thenReturn(playlistWithItemWithoutCoverExtension)

            /* When */
            rest
                    .post()
                    .uri("/api/v1/playlists")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""{"name":"${playlist.name}"}""")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                            "id":"9706ba78-2df2-4b37-a573-04367dc6f0ea",
                            "name":"foo",
                            "items":[{
                                "id": "c42d2a59-46e6-4c1d-b0fb-2b47d389b370",
                                "title": "a title",
                                "proxyURL": "/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/a-title.mp3",
                                "description": "a desc",
                                "mimeType": "audio/mp3",
                                "podcast": {
                                  "id": "3ba6411c-8fb9-4e24-afb1-adbad9a023e0",
                                  "title": "a podcast"
                                },
                                "cover": {
                                  "id": "0882344b-fcaf-4332-9ab8-47e78921f929",
                                  "width": 123,
                                  "height": 456,
                                  "url": "/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/cover.jpg"
                                }
                              }]
                        }""")
                    }
        }
    }

    @Nested
    @DisplayName("should find all")
    inner class ShouldFindAll {

        @Test
        fun `with no watch list`() {
            /* Given */
            whenever(service.findAll()).thenReturn(emptyList())
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
            whenever(service.findAll()).thenReturn(listOf(
                    Playlist(UUID.fromString("05621536-b211-4736-a1ed-94d7ad494fe0"), "first"),
                    Playlist(UUID.fromString("6e15b195-7a1f-43e8-bc06-bf88b7f865f8"), "second"),
                    Playlist(UUID.fromString("37d09949-6ae0-4b8b-8cc9-79ffd541e51b"), "third")
            )
            )

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

        @Nested
        @DisplayName("as json")
        inner class AsJson {

            @Test
            fun `with no items`() {
                /* Given */
                val playlist = PlaylistWithItems(id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"), name = "foo", items = emptyList())
                whenever(service.findById(playlist.id)).thenReturn(playlist)
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
                                        fileName = Path("file.mp3"),
                                        length = 10L,
                                        pubDate = OffsetDateTime.of(2019, 10, 27, 10, 10, 10, 10, ZoneOffset.UTC),
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
                whenever(service.findById(playlist.id)).thenReturn(playlist)
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
                                      "proxyURL":"/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/a-title.mp3",
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
                                        fileName = Path("file.mp3"),
                                        length = 10L,
                                        pubDate = OffsetDateTime.of(2019, 10, 27, 10, 10, 10, 10, ZoneOffset.UTC),
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
                                        fileName = Path("file2.mp3"),
                                        length = 10L,
                                        pubDate = OffsetDateTime.of(2019, 10, 27, 10, 10, 10, 10, ZoneOffset.UTC),
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
                whenever(service.findById(playlist.id)).thenReturn(playlist)
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
                                      "proxyURL":"/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/a-title.mp3",
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
                                      "proxyURL":"/api/v1/podcasts/35d04720-1bc3-476b-b7b0-494a15adf45e/items/4b48996c-686f-4339-b94e-f9595094f2ea/2-a-title.mp3",
                                      "title":"2 a title"
                                   }
                                ]
                            }""")
                        }
            }
        }

        @Nested
        @DisplayName("as rss")
        inner class AsRss {

            val item = PlaylistWithItems.Item(
                id = UUID.fromString("c42d2a59-46e6-4c1d-b0fb-2b47d389b370"),
                title = "a title",
                description = "a desc",
                mimeType = "audio/mp3",
                fileName = Path("file.mp3"),
                length = 10L,
                pubDate = OffsetDateTime.of(2019, 10, 27, 10, 10, 10, 10, ZoneOffset.UTC),
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
            val playlist = PlaylistWithItems(
                id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"),
                name = "foo",
                items = listOf(item)
            )

            @Test
            fun `with 1 item`() {
                /* Given */
                whenever(service.findById(playlist.id)).thenReturn(playlist)

                /* When */
                rest
                        .get()
                        .uri("/api/v1/playlists/{id}/rss", playlist.id)
                        .header("Host", "foo.com")
                        .header("X-Forwarded-Proto", "https")
                        .exchange()
                        /* Then */
                        .expectHeader()
                        .contentType(MediaType.APPLICATION_XML)
                        .expectStatus().isOk
                        .expectBody()
                        .xml("""
                            <?xml version="1.0" encoding="UTF-8"?>
                            <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
                              <channel>
                                <title>foo</title>
                                <link>https://foo.com/api/v1/playlists/9706ba78-2df2-4b37-a573-04367dc6f0ea/rss</link>
                                <item>
                                  <title>a title</title>
                                  <description>a desc</description>
                                  <pubDate>Sun, 27 Oct 2019 10:10:10 GMT</pubDate>
                                  <itunes:explicit>No</itunes:explicit>
                                  <itunes:subtitle>a title</itunes:subtitle>
                                  <itunes:summary>a desc</itunes:summary>
                                  <guid>https://foo.com/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370</guid>
                                  <itunes:image>https://foo.com/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/cover.png</itunes:image>
                                  <media:thumbnail xmlns:media="http://search.yahoo.com/mrss/" url="https://foo.com/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/cover.png" />
                                  <enclosure url="https://foo.com/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/a-title.mp3" length="10" type="audio/mp3" />
                                </item>
                              </channel>
                            </rss>
                        """.trimIndent())
            }

            @Test
            fun `with 1 item without cover extention`() {
                /* Given */
                val playlistWithItemWithoutCoverExtension = playlist.copy(
                    items = listOf(
                        item.copy(
                            cover = item.cover.copy(
                                url = URI("https://foo.com/bar/podcast/image")
                            )
                        )
                    )
                )
                whenever(service.findById(playlistWithItemWithoutCoverExtension.id))
                    .thenReturn(playlistWithItemWithoutCoverExtension)

                /* When */
                rest
                    .get()
                    .uri("/api/v1/playlists/{id}/rss", playlist.id)
                    .header("Host", "foo.com")
                    .header("X-Forwarded-Proto", "https")
                    .exchange()
                    /* Then */
                    .expectHeader()
                    .contentType(MediaType.APPLICATION_XML)
                    .expectStatus().isOk
                    .expectBody()
                    .xml("""
                            <?xml version="1.0" encoding="UTF-8"?>
                            <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
                              <channel>
                                <title>foo</title>
                                <link>https://foo.com/api/v1/playlists/9706ba78-2df2-4b37-a573-04367dc6f0ea/rss</link>
                                <item>
                                  <title>a title</title>
                                  <description>a desc</description>
                                  <pubDate>Sun, 27 Oct 2019 10:10:10 GMT</pubDate>
                                  <itunes:explicit>No</itunes:explicit>
                                  <itunes:subtitle>a title</itunes:subtitle>
                                  <itunes:summary>a desc</itunes:summary>
                                  <guid>https://foo.com/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370</guid>
                                  <itunes:image>https://foo.com/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/cover.jpg</itunes:image>
                                  <media:thumbnail xmlns:media="http://search.yahoo.com/mrss/" url="https://foo.com/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/cover.jpg" />
                                  <enclosure url="https://foo.com/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/a-title.mp3" length="10" type="audio/mp3" />
                                </item>
                              </channel>
                            </rss>
                        """.trimIndent())
            }

            @Test
            fun `with item not found`() {
                /* Given */
                whenever(service.findById(playlist.id)).thenReturn(null)

                /* When */
                rest
                    .get()
                    .uri("/api/v1/playlists/{id}/rss", playlist.id)
                    .header("Host", "foo.com")
                    .header("X-Forwarded-Proto", "https")
                    .exchange()
                    /* Then */
                    .expectStatus().isNotFound
            }

        }
    }

    @Nested
    @DisplayName("should delete")
    inner class ShouldDelete {

        @Test
        fun `by id`() {
            /* Given */
            val id = UUID.randomUUID()
            doNothing().whenever(service).deleteById(id)
            /* When */
            rest
                    .delete()
                    .uri("/api/v1/playlists/{id}", id)
                    .exchange()
                    /* Then */
                    .expectStatus().isNoContent
        }
    }

    @Nested
    @DisplayName("should add")
    inner class ShouldAdd {

        @Test
        fun `to playlist`() {
            /* Given */
            val item = PlaylistWithItems.Item(
                    id = UUID.fromString("c42d2a59-46e6-4c1d-b0fb-2b47d389b370"),
                    title = "a title",
                    description = "a desc",
                    mimeType = "audio/mp3",
                    fileName = Path("file.mp3"),
                    length = 10L,
                    pubDate = OffsetDateTime.of(2019, 10, 27, 10, 10, 10, 10, ZoneOffset.UTC),
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
            val playlist = PlaylistWithItems(
                    id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"),
                    name = "foo",
                    items = listOf(item)
            )
            whenever(service.addToPlaylist(playlist.id, item.id)).thenReturn(playlist)

            /* When */
            rest
                    .post()
                    .uri("/api/v1/playlists/{id}/items/{itemId}", playlist.id, item.id)
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
                                  "proxyURL":"/api/v1/podcasts/3ba6411c-8fb9-4e24-afb1-adbad9a023e0/items/c42d2a59-46e6-4c1d-b0fb-2b47d389b370/a-title.mp3",
                                  "title":"a title"
                               }
                            ]
                        }""")
                    }
        }
    }

    @Nested
    @DisplayName("should remove")
    inner class ShouldRemove {

        @Test
        fun `from playlist`() {
            /* Given */
            val playlist = PlaylistWithItems(
                id = UUID.fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"),
                name = "foo",
                items = emptyList()
            )
            val itemId = UUID.fromString("dd5b4b49-7fd8-4d7b-a406-e8e451ef7792")
            whenever(service.removeFromPlaylist(playlist.id, itemId))
                .thenReturn(playlist)

            /* When */
            rest
                    .delete()
                    .uri("/api/v1/playlists/{id}/items/{itemId}", playlist.id, itemId)
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
    }
}
