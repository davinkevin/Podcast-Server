package com.github.davinkevin.podcastserver.update.updaters.youtube

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.manager.worker.CoverFromUpdate
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.service.properties.Api
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.test.StepVerifier
import java.net.URI
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by kevin on 16/09/2018
 */
@ExtendWith(SpringExtension::class)
class YoutubeByXmlUpdaterTest(
        @Autowired private val updater: YoutubeByXmlUpdater
) {

    @TestConfiguration
    @Import(
            WebClientAutoConfiguration::class,
            WebClientConfig::class,
            YoutubeUpdaterConfig::class,
            JacksonAutoConfiguration::class
    )
    class LocalTestConfiguration {
        @Bean fun remapToLocalHost() = remapToMockServer("www.youtube.com")
        @Bean fun api(): Api = Api()
    }

    private val channel = PodcastToUpdate(
            id = UUID.randomUUID(),
            url = URI("https://www.youtube.com/user/joueurdugrenier"),
            signature = "old_signature"
    )

    private val playlist = PodcastToUpdate(
            id = UUID.randomUUID(),
            url = URI("https://www.youtube.com/playlist?list=PLAD454F0807B6CB80"),
            signature = "old_signature"
    )

    @Nested
    @DisplayName("should find items")
    @ExtendWith(MockServer::class)
    inner class ShouldFind {

        @Nested
        @DisplayName("in a channel")
        inner class InAChannel {

            @Test
            fun `with no items`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/user/joueurdugrenier")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.html"))))
                    stubFor(get("/feeds/videos.xml?channel_id=UC_yP2DpIgs5Y1uWC0T03Chw")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.channel.with-0-item.xml"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(channel))
                        /* Then */
                        .expectSubscription()
                        .expectNextCount(0)
                        .verifyComplete()
            }

            @Test
            fun `with 1 item`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/user/joueurdugrenier")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.html"))))
                    stubFor(get("/feeds/videos.xml?channel_id=UC_yP2DpIgs5Y1uWC0T03Chw")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.channel.with-1-item.xml"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(channel))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.url).isEqualTo(URI("https://www.youtube.com/watch?v=Xos2M-gTf6g"))
                            assertThat(it.cover).isEqualTo(CoverFromUpdate(480, 360, URI("https://i1.ytimg.com/vi/Xos2M-gTf6g/hqdefault.jpg")))
                            assertThat(it.title).isEqualTo("Joueur du grenier - Des jeux Color Dreams - NES")
                            assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2012, 10, 11, 16, 0, 9, 0, ZoneId.of("UTC")))
                            assertThat(it.description).isEqualTo("""Salut tout le monde ! Voici le 35ème test du grenier avec 2 jeux non licenciés édités par Color Dreams, Raid 2020 et Silent assault sur NES. j'espère que ca vous plaira, un test plus classique avec moins d'effets. Il en faut pour tout les goûts.""")
                        }
                        .verifyComplete()
            }

            @Test
            fun `with all items`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/user/joueurdugrenier")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.html"))))
                    stubFor(get("/feeds/videos.xml?channel_id=UC_yP2DpIgs5Y1uWC0T03Chw")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.channel.xml"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(channel))
                        /* Then */
                        .expectSubscription()
                        .expectNextCount(15)
                        .verifyComplete()
            }
        }

        @Nested
        @DisplayName("in a playlist")
        inner class InAPlaylist {

            @Test
            fun `with no items`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/feeds/videos.xml?playlist_id=PLAD454F0807B6CB80")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.playlist.with-0-item.xml"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(playlist))
                        /* Then */
                        .expectSubscription()
                        .expectNextCount(0)
                        .verifyComplete()
            }

            @Test
            fun `with 1 item`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/feeds/videos.xml?playlist_id=PLAD454F0807B6CB80")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.playlist.with-1-item.xml"))))
                }

                /* When */
                StepVerifier.create(updater.findItems(playlist))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.url).isEqualTo(URI("https://www.youtube.com/watch?v=Xos2M-gTf6g"))
                            assertThat(it.cover).isEqualTo(CoverFromUpdate(480, 360, URI("https://i1.ytimg.com/vi/Xos2M-gTf6g/hqdefault.jpg")))
                            assertThat(it.title).isEqualTo("Joueur du grenier - Des jeux Color Dreams - NES")
                            assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2012, 10, 11, 16, 0, 9, 0, ZoneId.of("UTC")))
                            assertThat(it.description).isEqualTo("""Salut tout le monde ! Voici le 35ème test du grenier avec 2 jeux non licenciés édités par Color Dreams, Raid 2020 et Silent assault sur NES. j'espère que ca vous plaira, un test plus classique avec moins d'effets. Il en faut pour tout les goûts.""")
                        }
                        .verifyComplete()
            }

            @Test
            fun `with all items`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/feeds/videos.xml?playlist_id=PLAD454F0807B6CB80")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.playlist.xml"))))
                }


                /* When */
                StepVerifier.create(updater.findItems(playlist))
                        /* Then */
                        .expectSubscription()
                        .expectNextCount(15)
                        .verifyComplete()
            }
        }

    }

    @Nested
    @DisplayName("should sign")
    @ExtendWith(MockServer::class)
    inner class ShouldSign {

        @Nested
        @DisplayName("a playlist")
        inner class APlaylist {

            @Test
            fun `with 0 item`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/feeds/videos.xml?playlist_id=PLAD454F0807B6CB80")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.playlist.with-0-item.xml"))))
                }

                /* When */
                StepVerifier.create(updater.signatureOf(playlist.url))
                        /* Then */
                        .expectSubscription()
                        .expectNext("")
                        .verifyComplete()
            }

            @Test
            fun `with all items`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/feeds/videos.xml?playlist_id=PLAD454F0807B6CB80")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.playlist.xml"))))
                }

                /* When */
                StepVerifier.create(updater.signatureOf(playlist.url))
                        /* Then */
                        .expectSubscription()
                        .expectNext("e134f42e363e1b763518e6af46fb3a96")
                        .verifyComplete()
            }
        }

        @Nested
        @DisplayName("a channel")
        inner class AChannel {

            @Test
            fun `with no item`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/user/joueurdugrenier")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.html"))))
                    stubFor(get("/feeds/videos.xml?channel_id=UC_yP2DpIgs5Y1uWC0T03Chw")
                        .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.channel.with-0-item.xml"))))
                }

                /* When */
                StepVerifier.create(updater.signatureOf(channel.url))
                        /* Then */
                        .expectSubscription()
                        .expectNext("")
                        .verifyComplete()
            }

            @Test
            fun `with all items`(backend: WireMockServer) {
                /* Given */
                backend.apply {
                    stubFor(get("/user/joueurdugrenier")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.html"))))
                    stubFor(get("/feeds/videos.xml?channel_id=UC_yP2DpIgs5Y1uWC0T03Chw")
                            .willReturn(okJson(fileAsString("/remote/podcast/youtube/joueurdugrenier.channel.xml"))))
                }

                /* When */
                StepVerifier.create(updater.signatureOf(channel.url))
                        /* Then */
                        .expectSubscription()
                        .expectNext("af35e61ec15b5356cb2ed7c22f5e7a92")
                        .verifyComplete()
            }
        }
    }

    @Test
    fun `should return Youtube type`() {
        /* Given */
        /* When */
        val type = updater.type()
        /* Then */
        assertThat(type.key).isEqualTo("Youtube")
        assertThat(type.name).isEqualTo("Youtube")
    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

        @Test
        fun `should be compatible`() {
            /* Given */
            val url = "https://www.youtube.com/user/joueurdugrenier"
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @Test
        fun `should not be compatible`() {
            /* Given */
            val url = "grpc://foo.bar.com"
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

        @Test
        fun `should not be compatible because url is null`() {
            /* Given */
            val url = null
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }
    }

    @Nested
    @DisplayName("blocking")
    inner class Blocking {

        @Test
        fun `should not serve items with blocking method`() {
            /* Given */
            /* When */
            assertThatThrownBy { updater.blockingFindItems(channel) }
                    /* Then */
                    .hasMessage("An operation is not implemented: not required anymore...")
        }

        @Test
        fun `should not sign podcast with blocking method`() {
            /* Given */
            /* When */
            assertThatThrownBy { updater.blockingSignatureOf(channel.url) }
                    /* Then */
                    .hasMessage("An operation is not implemented: not required anymore...")
        }
    }
}
