package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Created by kevin on 17/09/2026
 *
 * Built by hand rather than through the spring context, because the video types are
 * read once at bean creation: covering `all`, one type and several types in the same
 * class is impossible with a class level `@TestPropertySource`. The wiring itself is
 * covered by `TwitchConfigTest`.
 */
@ExtendWith(MockServer::class)
class TwitchUpdaterTest {

    private val image = mock<ImageService>()

    private fun updaterWith(videoTypes: List<String?> = listOf(null)): TwitchUpdater {
        val rc = RestClient.builder().baseUrl("http://localhost:5555").build()

        return TwitchUpdater(
            rc = rc,
            token = TwitchTokenProvider(
                rc = rc,
                clientId = "client-id",
                clientSecret = "client-secret",
                clock = Clock.system(ZoneId.of("UTC")),
            ),
            image = image,
            videoTypes = videoTypes,
            registry = SimpleMeterRegistry(),
        )
    }

    private val podcast = PodcastToUpdate(
        id = UUID.fromString("dd16b2eb-657e-4064-b13f-4f81c4a34f7c"),
        url = URI("https://www.twitch.tv/twitchdev"),
        signature = "old_signature",
    )

    private fun WireMockServer.stubToken() = stubFor(
        post("/oauth2/token").willReturn(okJson(fileAsString("/remote/podcast/twitch/token.json")))
    )

    private fun WireMockServer.stubUser(fixture: String = "user.json") = stubFor(
        get(urlPathEqualTo("/helix/users"))
            .withQueryParam("login", equalTo("twitchdev"))
            .willReturn(okJson(fileAsString("/remote/podcast/twitch/$fixture")))
    )

    private fun WireMockServer.stubVideos(fixture: String) = stubFor(
        get(urlPathEqualTo("/helix/videos"))
            .willReturn(okJson(fileAsString("/remote/podcast/twitch/$fixture")))
    )

    private fun coverIsFetched() = whenever(image.fetchCoverInformation(any()))
        .thenAnswer { CoverInformation(1280, 720, it.getArgument(0)) }

    @Nested
    @DisplayName("should find items")
    inner class ShouldFindItems {

        @Test
        fun `of a channel`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubVideos("videos.json")
            coverIsFetched()

            /* When */
            val items = updaterWith().findItems(podcast)

            /* Then */
            assertThat(items).hasSize(2)
            assertAll {
                val first = items.first()
                assertThat(first.title).isEqualTo("Extensions Monthly")
                assertThat(first.url).isEqualTo(URI("https://www.twitch.tv/videos/335921246"))
                assertThat(first.pubDate).isEqualTo(ZonedDateTime.parse("2019-01-10T19:30:00Z"))
                assertThat(first.mimeType).isEqualTo("video/mp4")
                assertThat(first.guid).isNull()

                val second = items.last()
                assertThat(second.title).isEqualTo("Twitch Developers 101")
                assertThat(second.description).isEqualTo("Welcome to Twitch development!")
            }
        }

        @Test
        fun `with the cover placeholders substituted`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubVideos("videos.json")
            coverIsFetched()

            /* When */
            val items = updaterWith().findItems(podcast)

            /* Then */
            assertThat(items.map { it.cover!!.url.toASCIIString() }).allSatisfy {
                assertThat(it).doesNotContain("%").endsWith("thumb-1280x720.jpg")
            }
        }

        @Test
        fun `falling back on created_at when published_at is blank`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubVideos("videos.without-published-at.json")
            coverIsFetched()

            /* When */
            val items = updaterWith().findItems(podcast)

            /* Then */
            assertThat(items.first().pubDate).isEqualTo(ZonedDateTime.parse("2018-11-14T20:00:00Z"))
        }

        @Test
        fun `with nothing when the channel has no video`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubVideos("videos.empty.json")

            /* When */
            val items = updaterWith().findItems(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `by following the pagination cursor`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubFor(
                get(urlPathEqualTo("/helix/videos"))
                    .withQueryParam("after", absent())
                    .willReturn(okJson(fileAsString("/remote/podcast/twitch/videos.page-1.json")))
            )
            backend.stubFor(
                get(urlPathEqualTo("/helix/videos"))
                    .withQueryParam("after", equalTo("eyJiIjpudWxsLCJhIjp7IkN1cnNvciI6ImNvb2tpZSJ9fQ"))
                    .willReturn(okJson(fileAsString("/remote/podcast/twitch/videos.page-2.json")))
            )
            coverIsFetched()

            /* When */
            val items = updaterWith().findItems(podcast)

            /* Then */
            assertThat(items.map { it.title }).containsExactly("first page", "second page")
        }

        @Test
        fun `without going past the page cap`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            // every page hands back a cursor, so only the cap can stop the walk
            backend.stubVideos("videos.page-1.json")
            coverIsFetched()

            /* When */
            updaterWith().findItems(podcast)

            /* Then */
            backend.verify(10, getRequestedFor(urlPathEqualTo("/helix/videos")))
        }

        @Test
        fun `and fails when the login is unknown`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser("user.unknown.json")

            /* When */
            assertThatThrownBy { updaterWith().findItems(podcast) }
                /* Then */
                .hasMessage("user not found")
        }
    }

    @Nested
    @DisplayName("should exclude a broadcast still in progress")
    inner class ShouldExcludeABroadcastStillInProgress {

        @Test
        fun `from the items`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubVideos("videos.with-live.json")
            coverIsFetched()

            /* When */
            val items = updaterWith().findItems(podcast)

            /* Then */
            assertThat(items.map { it.title }).containsExactly("Twitch Developers 101")
        }

        @Test
        fun `from the signature`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubVideos("videos.with-live.json")

            /* When */
            val signature = updaterWith().signatureOf(podcast.url)

            /* Then */
            assertThat(signature).isEqualTo(md5Of("335921245"))
        }
    }

    @Nested
    @DisplayName("should sign")
    inner class ShouldSign {

        @Test
        fun `from the first page only`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubVideos("videos.page-1.json")

            /* When */
            val signature = updaterWith().signatureOf(podcast.url)

            /* Then */
            assertAll {
                assertThat(signature).isEqualTo(md5Of("page-1-video"))
                backend.verify(1, getRequestedFor(urlPathEqualTo("/helix/videos")))
            }
        }

        @Test
        fun `with an empty signature when the channel has no video`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubVideos("videos.empty.json")

            /* When */
            val signature = updaterWith().signatureOf(podcast.url)

            /* Then */
            assertThat(signature).isEqualTo("")
        }
    }

    @Nested
    @DisplayName("should ask helix for the configured video types")
    inner class ShouldAskHelixForTheConfiguredVideoTypes {

        @Test
        fun `with no type parameter at all by default`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubVideos("videos.empty.json")

            /* When */
            updaterWith(videoTypes = listOf(null)).findItems(podcast)

            /* Then */
            backend.verify(
                1, getRequestedFor(urlPathEqualTo("/helix/videos")).withQueryParam("type", absent())
            )
        }

        @Test
        fun `with a single call for a single type`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubVideos("videos.highlight.json")
            coverIsFetched()

            /* When */
            val items = updaterWith(videoTypes = listOf("highlight")).findItems(podcast)

            /* Then */
            assertAll {
                assertThat(items.map { it.title }).containsExactly("a highlight")
                backend.verify(1, getRequestedFor(urlPathEqualTo("/helix/videos")))
                backend.verify(
                    1,
                    getRequestedFor(urlPathEqualTo("/helix/videos")).withQueryParam("type", equalTo("highlight"))
                )
            }
        }

        @Test
        fun `with one call per type, merged newest first`(backend: WireMockServer) {
            /* Given */
            backend.stubToken()
            backend.stubUser()
            backend.stubFor(
                get(urlPathEqualTo("/helix/videos"))
                    .withQueryParam("type", equalTo("highlight"))
                    .willReturn(okJson(fileAsString("/remote/podcast/twitch/videos.highlight.json")))
            )
            backend.stubFor(
                get(urlPathEqualTo("/helix/videos"))
                    .withQueryParam("type", equalTo("upload"))
                    .willReturn(okJson(fileAsString("/remote/podcast/twitch/videos.upload.json")))
            )
            coverIsFetched()

            /* When */
            val items = updaterWith(videoTypes = listOf("highlight", "upload")).findItems(podcast)

            /* Then */
            assertAll {
                assertThat(items.map { it.title }).containsExactly("an upload", "a highlight")
                backend.verify(2, getRequestedFor(urlPathEqualTo("/helix/videos")))
            }
        }
    }

    @Test
    fun `should return twitch type`() {
        /* Given */
        /* When */
        val twitchType = updaterWith().type()

        /* Then */
        assertAll {
            assertThat(twitchType.key).isEqualTo("Twitch")
            assertThat(twitchType.name).isEqualTo("Twitch")
        }
    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

        @Test
        fun `should be compatible with a channel url`() {
            /* Given */
            /* When */
            val compatibility = updaterWith().compatibility("https://www.twitch.tv/twitchdev/videos")
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @Test
        fun `should not be compatible with a single vod url`() {
            /* Given */
            /* When */
            val compatibility = updaterWith().compatibility("https://www.twitch.tv/videos/335921245")
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }
    }
}

private fun md5Of(vararg ids: String) =
    DigestUtils.md5DigestAsHex(ids.sorted().joinToString(", ").toByteArray())
