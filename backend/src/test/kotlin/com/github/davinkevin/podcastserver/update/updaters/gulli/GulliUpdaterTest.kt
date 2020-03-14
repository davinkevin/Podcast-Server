package com.github.davinkevin.podcastserver.update.updaters.gulli

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.manager.worker.CoverFromUpdate
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
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
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by kevin on 14/03/2020
 */
@ExtendWith(SpringExtension::class)
class GulliUpdaterTest(
        @Autowired private val updater: GulliUpdater
) {

    @TestConfiguration
    @Import(
            WebClientAutoConfiguration::class,
            WebClientConfig::class,
            GulliUpdaterConfig::class,
            JacksonAutoConfiguration::class
    )
    class LocalTestConfiguration {
        @Bean fun remapToLocalHost() = remapToMockServer("replay.gulli.fr")
    }


    @MockBean
    lateinit var imageService: ImageServiceV2

    private val podcast = PodcastToUpdate(
            id = UUID.randomUUID(),
            url = URI("https://replay.gulli.fr/dessins-animes/Pokemon7"),
            signature = "old_signature"
    )

    @Nested
    @DisplayName("should find items")
    @ExtendWith(MockServer::class)
    inner class ShouldFindItems {

        @Test
        fun `with 1 item`(backend: WireMockServer) {
            /* Given */
            whenever(imageService.fetchCoverInformation(any()))
                    .thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            backend.apply {
                stubFor(get("/dessins-animes/Pokemon7")
                        .willReturn(okJson(fileAsString("/remote/podcast/gulli/pokemon.with-1-item.html"))))
                stubFor(get("/dessins-animes/Pokemon7/VOD69210373530000")
                        .willReturn(okJson(fileAsString("/remote/podcast/gulli/VOD69210373530000.html"))))
            }

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.url).isEqualTo(URI("https://replay.gulli.fr/dessins-animes/Pokemon7/VOD69210373530000"))
                        assertThat(it.cover).isEqualTo(CoverFromUpdate(100, 200, URI("https://fake.url.com/img.png")))
                        assertThat(it.title).isEqualTo("Une petite balle capricieuse")
                        assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2020, 3, 14, 19, 4, 44, 0, ZoneId.of("Europe/Paris")))
                        assertThat(it.description).isEqualTo("""Les élèves de l&#039;École Pokémon rencontrent une talentueuse golfeuse du nom de Kahili et son Bazoukan, &quot; Bazouki &quot;. Kahili traverse une mauvaise passe et le bruit court qu&#039;elle pourrait se retirer du circuit professionnel mais lorsque nos héros lui demandent de faire un parcours avec eux, elle leur donne gentiment de précieux conseils. Sur le terrain, ils sont rejoints par une figure familière, qui se prétend Maître Caddy et met Kahili au défi de viser un trou incroyablement difficile, ce qu&#039;elle fait... avec l&#039;aide de Bazouki ! Ces bons moments passés avec nos héros encouragent Kahili à continuer le Golf Pokémon et Bazouki en est ravi !""")
                    }
                    .verifyComplete()
        }

        @Test
        fun `with all items`(backend: WireMockServer) {
            /* Given */
            whenever(imageService.fetchCoverInformation(any()))
                    .thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            backend.apply {
                stubFor(get("/dessins-animes/Pokemon7")
                        .willReturn(okJson(fileAsString("/remote/podcast/gulli/pokemon.html"))))
                stubFor(get("/dessins-animes/Pokemon7/VOD69210373530000")
                        .willReturn(okJson(fileAsString("/remote/podcast/gulli/VOD69210373530000.html"))))
                stubFor(get("/dessins-animes/Pokemon7/VOD69214276976000")
                        .willReturn(okJson(fileAsString("/remote/podcast/gulli/VOD69214276976000.html"))))
                stubFor(get("/dessins-animes/Pokemon7/VOD69214277046000")
                        .willReturn(okJson(fileAsString("/remote/podcast/gulli/VOD69214277046000.html"))))
                stubFor(get("/dessins-animes/Pokemon7/VOD69218401100000")
                        .willReturn(okJson(fileAsString("/remote/podcast/gulli/VOD69218401100000.html"))))
                stubFor(get("/dessins-animes/Pokemon7/VOD69218401170000")
                        .willReturn(okJson(fileAsString("/remote/podcast/gulli/VOD69218401170000.html"))))
                stubFor(get("/dessins-animes/Pokemon7/VOD69262302148000")
                        .willReturn(okJson(fileAsString("/remote/podcast/gulli/VOD69262302148000.html"))))
            }

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(6)
                    .verifyComplete()
        }

    }

    @Nested
    @DisplayName("should sign")
    @ExtendWith(MockServer::class)
    inner class ShouldSign {

        @Test
        fun `with no item`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/dessins-animes/Pokemon7")
                    .willReturn(okJson(fileAsString("/remote/podcast/gulli/pokemon.with-no-item.html")))
            )

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("")
                    .verifyComplete()
        }

        @Test
        fun `with 1 item`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/dessins-animes/Pokemon7")
                    .willReturn(okJson(fileAsString("/remote/podcast/gulli/pokemon.with-1-item.html")))
            )

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("67e44194d6aa62875e04fea1f2ffc9cc")
                    .verifyComplete()
        }

        @Test
        fun `with all items`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("/dessins-animes/Pokemon7")
                    .willReturn(okJson(fileAsString("/remote/podcast/gulli/pokemon.html")))
            )

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("b8119ec4c3128965c098bc0d0dad8237")
                    .verifyComplete()
        }
    }


    @Test
    fun `should return Dailymotion type`() {
        /* Given */
        /* When */
        val type = updater.type()
        /* Then */
        assertThat(type.key).isEqualTo("Gulli")
        assertThat(type.name).isEqualTo("Gulli")
    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

        @Test
        fun `should be compatible`() {
            /* Given */
            val url = "https://replay.gulli.fr/dessins-animes/Pokemon7"
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
            assertThatThrownBy { updater.blockingFindItems(podcast) }
                    /* Then */
                    .hasMessage("An operation is not implemented: not required anymore...")
        }

        @Test
        fun `should not sign podcast with blocking method`() {
            /* Given */
            /* When */
            assertThatThrownBy { updater.blockingSignatureOf(podcast.url) }
                    /* Then */
                    .hasMessage("An operation is not implemented: not required anymore...")
        }
    }

}
