package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.extension.mockmvc.MockMvcRestExceptionConfiguration
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.io.path.Path

@WebMvcTest(controllers = [PlaylistXmlHandler::class])
@Import(PlaylistRoutingConfig::class, PlaylistHandler::class, PlaylistXmlHandler::class, MockMvcRestExceptionConfiguration::class)
class PlaylistXmlHandlerTest (
    @Autowired val rest: WebTestClient
) {

    @MockBean private lateinit var service: PlaylistService

    @Nested
    @DisplayName("should find by id")
    inner class ShouldFindById {

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
                                <itunes:image>https://foo.com/api/v1/playlists/9706ba78-2df2-4b37-a573-04367dc6f0ea/cover.png</itunes:image>
                                <image>
                                    <url>https://foo.com/api/v1/playlists/9706ba78-2df2-4b37-a573-04367dc6f0ea/cover.png</url>
                                    <height>600</height>
                                    <width>600</width>
                                </image>
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
                                <itunes:image>https://foo.com/api/v1/playlists/9706ba78-2df2-4b37-a573-04367dc6f0ea/cover.png</itunes:image>
                                <image>
                                    <url>https://foo.com/api/v1/playlists/9706ba78-2df2-4b37-a573-04367dc6f0ea/cover.png</url>
                                    <height>600</height>
                                    <width>600</width>
                                </image>
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
}