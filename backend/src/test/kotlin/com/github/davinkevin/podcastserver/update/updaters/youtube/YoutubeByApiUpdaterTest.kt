package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.remapToMockServer
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
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import java.util.*

/**
 * Created by kevin on 31/08/2019
 */
@ExtendWith(SpringExtension::class)
@TestPropertySource(properties = [
    "podcastserver.api.youtube = key"
])
class YoutubeByApiUpdaterTest(
        @Autowired val updater: YoutubeByApiUpdater
) {

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
            StepVerifier.create(updater.findItems(podcast))
                    .expectSubscription()
                    /* Then */
                    .expectNextCount(91)
                    .verifyComplete()
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
            StepVerifier.create(updater.findItems(podcast))
                    .expectSubscription()
                    /* Then */
                    .expectNextCount(91)
                    .verifyComplete()
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
            StepVerifier.create(updater.findItems(podcast))
                    .expectSubscription()
                    /* Then */
                    .expectNextCount(91)
                    .verifyComplete()
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
            StepVerifier.create(updater.findItems(podcast))
                .expectSubscription()
                /* Then */
                .expectNextCount(91)
                .verifyComplete()
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
            StepVerifier.create(updater.findItems(podcast))
                .expectSubscription()
                /* Then */
                .expectNextCount(91)
                .verifyComplete()
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
            StepVerifier.create(updater.findItems(podcast))
                    .expectSubscription()
                    /* Then */
                    .expectNextCount(50)
                    .verifyComplete()
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
            StepVerifier.create(updater.findItems(podcast))
                    .expectSubscription()
                    /* Then */
                    .expectErrorMessage("channel id not found")
                    .verify()
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
            StepVerifier.create(updater.findItems(podcast))
                    .expectSubscription()
                    /* Then */
                    .verifyComplete()
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
            StepVerifier.create(updater.signatureOf(podcast.url))
                    .expectSubscription()
                    /* Then */
                    .assertNext {
                        assertThat(it).isEqualTo("64cc064a14dba90a0df24218db758479")
                    }
                    .verifyComplete()
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
            StepVerifier.create(updater.signatureOf(podcast.url))
                    .expectSubscription()
                    /* Then */
                    .assertNext {
                        assertThat(it).isEqualTo("")
                    }
                    .verifyComplete()
        }
    }

    @Test
    fun `should return youtube type`() {
        val type = updater.type()
        assertThat(type.name).isEqualTo("Youtube")
        assertThat(type.key).isEqualTo("Youtube")
    }

    @TestConfiguration
    @Import(
            YoutubeUpdaterConfig::class,
            WebClientAutoConfiguration::class,
            JacksonAutoConfiguration::class,
            WebClientConfig::class
    )
    class LocalTestConfiguration {
        @Bean fun remapGoogleApiToMock() = remapToMockServer("www.googleapis.com")
        @Bean fun remapYoutubeToMock() = remapToMockServer("www.youtube.com")

    }

    data class CompositeType(val idx: Int, val other: Int) {
        companion object {
            fun from(idx: Int): CompositeType {
                val i = idx * idx
                val other = i.toString().length

                return CompositeType(i, other)
            }
        }
    }
    @Test
    fun `should test`() {
        /* Given */
        val p = Mono.just(2)
            .map { CompositeType(it, it.toString().length) }
            .expand { CompositeType.from(it.idx).toMono() }
            .map { it.other }
            .take(4)
        /* When */
        StepVerifier.create(p)
        /* Then */
            .expectNext(1)
            .expectNext(1)
            .expectNext(2)
            .expectNext(3)

            .verifyComplete()
    }
}

