package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.remapRestClientToMockServer
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI
import java.util.*

/**
 * Created by kevin on 31/08/2019
 */
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = [
    "podcastserver.api.youtube = key"
])
@AutoConfigureObservability
class YoutubeByApiUpdaterTest(
        @Autowired val updater: YoutubeByApiUpdater
) {

    @MockBean lateinit var image: ImageService

    @TestConfiguration
    @Import(
        YoutubeUpdaterConfig::class,
        RestClientAutoConfiguration::class,
        JacksonAutoConfiguration::class,
    )
    class LocalTestConfiguration {
        @Bean fun remapGoogleApiToMock() = remapRestClientToMockServer("www.googleapis.com")
        @Bean fun remapYoutubeToMock() = remapRestClientToMockServer("www.youtube.com")
    }

    @Nested
    @DisplayName("should find items")
    @ExtendWith(MockServer::class)
    inner class ShouldFindItems {

        @Test
        fun `from handle`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(UUID.randomUUID(), URI("https://www.youtube.com/@joueurdugrenier"), "noSign")

            backend.apply {
                stubFor(get("/@joueurdugrenier")
                    .willReturn(ok(fileAsString("/remote/podcast/youtube/joueurdugrenier.html"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key")
                        .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.json"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key&pageToken=CDIQAA")
                        .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.2.json"))))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(91)
        }

        @Test
        fun `from handle using c notation`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(UUID.randomUUID(), URI("https://www.youtube.com/c/joueurdugrenier"), "noSign")

            backend.apply {
                stubFor(get("/c/joueurdugrenier")
                    .willReturn(ok(fileAsString("/remote/podcast/youtube/joueurdugrenier.html"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key")
                        .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.json"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key&pageToken=CDIQAA")
                        .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.2.json"))))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(91)
        }

        @Test
        fun `from playlist`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(UUID.randomUUID(), URI("http://www.youtube.com/playlist?list=PL43OynbWaTMJf3TBZJ5A414D5f7UQ8kwL"), "noSign")

            backend.apply {
                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=PL43OynbWaTMJf3TBZJ5A414D5f7UQ8kwL&key=key")
                        .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.json"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=PL43OynbWaTMJf3TBZJ5A414D5f7UQ8kwL&key=key&pageToken=CDIQAA")
                        .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.2.json"))))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(91)
        }

        @Test
        fun `from userName`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(UUID.randomUUID(), URI("https://www.youtube.com/user/joueurdugrenier"), "noSign")

            backend.apply {
                stubFor(get("/youtube/v3/channels?key=key&forUsername=joueurdugrenier&part=id")
                    .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.id.json"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key")
                    .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.json"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key&pageToken=CDIQAA")
                    .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.2.json"))))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(91)
        }

        @Test
        fun `from channel`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(UUID.randomUUID(), URI("https://www.youtube.com/channel/UC_yP2DpIgs5Y1uWC0T03Chw"), "noSign")

            backend.apply {
                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key")
                    .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.json"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key&pageToken=CDIQAA")
                    .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.2.json"))))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(91)
        }

        @Test
        fun `and handle error on items`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(url = URI("https://www.youtube.com/user/joueurdugrenier"), id = UUID.randomUUID(), signature = "noSign")

            backend.apply {
                stubFor(get("/youtube/v3/channels?key=key&forUsername=joueurdugrenier&part=id")
                    .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.id.json"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key")
                        .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.json"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key&pageToken=CDIQAA")
                        .willReturn(notFound()))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(50)
        }

        @Test
        fun `and handle error on playlist id searching`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(
                url = URI("https://www.youtube.com/user/joueurdugrenier"),
                id = UUID.randomUUID(),
                signature = "noSign"
            )

            backend.apply {
                stubFor(get("/youtube/v3/channels?key=key&forUsername=joueurdugrenier&part=id")
                    .willReturn(okJson("""{ "items": [] }""")))
            }

            /* When */
            assertThatThrownBy { updater.findItems(podcast) }
                /* Then */
                .hasMessage("channel id not found")
        }

        @Test
        fun `and handle case where returned page is empty`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(
                url = URI("https://www.youtube.com/user/joueurdugrenier"),
                id = UUID.randomUUID(),
                signature = "noSign"
            )

            backend.apply {
                stubFor(get("/youtube/v3/channels?key=key&forUsername=joueurdugrenier&part=id")
                    .willReturn(ok()))
            }

            /* When */
            assertThatThrownBy { updater.findItems(podcast) }
                /* Then */
                .hasMessage("channel id not found")
        }

        @Test
        fun `from handle and returns error because page is empty`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(UUID.randomUUID(), URI("https://www.youtube.com/@joueurdugrenier"), "noSign")

            backend.apply {
                stubFor(get("/@joueurdugrenier")
                    .willReturn(ok()))
            }

            /* When */
            assertThatThrownBy { updater.findItems(podcast) }
                /* Then */
                .hasMessage("channel id not found")
        }

        @Test
        fun `and returns empty if no result`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(url = URI("https://www.youtube.com/user/joueurdugrenier"), id = UUID.randomUUID(), signature = "noSign")

            backend.apply {
                stubFor(get("/youtube/v3/channels?key=key&forUsername=joueurdugrenier&part=id")
                    .willReturn(okJson("""{ "items": [{ "kind": "youtube#channel", "etag": "P_Oq-TaAXb4OaAd4_3j2jDMUwAw", "id": "UC_yP2DpIgs5Y1uWC0T03Chw" }] }""")))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key")
                        .willReturn(okJson("""{
                             "kind": "youtube#playlistItemListResponse",
                             "etag": "mPrpS7Nrk6Ggi_P7VJ8-KsEOiIw/MREpXoOq5CSrGUjb1AQaTC451Tk",
                             "pageInfo": { "totalResults": 87, "resultsPerPage": 50 },
                             "items": []
                            }
                        """)))
            }

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(0)
        }
    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

        @DisplayName("should be compatible")
        @ParameterizedTest(name = "with {0}")
        @ValueSource(strings = [
            "http://www.youtube.com/channel/a-channel", "http://youtube.com/user/foo-User",
            "https://gdata.youtube.com/feeds/api/playlists/UE1987158913731", "https://another.youtube.com/bar-foo"
        ])
        fun `should be compatible with`(/* Given */ url: String) {
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @Test
        fun `should not be compatible`() {
            /* Given */
            val url = "http://foo.bar.com/"
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

    }

    @Nested
    @DisplayName("should sign")
    @ExtendWith(MockServer::class)
    inner class ShouldSign {

        @Test
        fun `with success`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(url = URI("https://www.youtube.com/user/joueurdugrenier"), id = UUID.randomUUID(), signature = "noSign")

            backend.apply {
                stubFor(get("/youtube/v3/channels?key=key&forUsername=joueurdugrenier&part=id")
                    .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.id.json"))))

                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key")
                        .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.json"))))
            }

            /* When */
            val sign = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(sign).isEqualTo("64cc064a14dba90a0df24218db758479")
        }

        @Test
        fun `with default value if error happen`(backend: WireMockServer) {
            /* Given */
            val podcast = PodcastToUpdate(url = URI("https://www.youtube.com/user/joueurdugrenier"), id = UUID.randomUUID(), signature = "noSign")

            backend.apply {
                stubFor(get("/youtube/v3/channels?key=key&forUsername=joueurdugrenier&part=id")
                    .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.id.json"))))
                stubFor(get("/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=key")
                        .willReturn(notFound()))
            }

            /* When */
            val sign = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(sign).isEqualTo("")
        }
    }

    @Test
    fun `should return youtube type`() {
        val type = updater.type()
        assertThat(type.name).isEqualTo("Youtube")
        assertThat(type.key).isEqualTo("Youtube")
    }
}

