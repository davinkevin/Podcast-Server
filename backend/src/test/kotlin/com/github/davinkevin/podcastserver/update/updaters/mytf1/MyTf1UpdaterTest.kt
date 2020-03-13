package com.github.davinkevin.podcastserver.update.updaters.mytf1

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
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
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

/**
 * Created by kevin on 11/03/2020
 */
@ExtendWith(SpringExtension::class)
class MyTf1UpdaterTest(
        @Autowired private val updater: MyTf1Updater
) {

    @TestConfiguration
    @Import(
            WebClientAutoConfiguration::class,
            WebClientConfig::class,
            MyTf1UpdaterConfig::class,
            JacksonAutoConfiguration::class
    )
    class LocalTestConfiguration {
        @Bean fun remapToLocalHost() = remapToMockServer("www.tf1.fr")
    }

    @MockBean lateinit var imageService: ImageService

    private val podcast = PodcastToUpdate(
            id = UUID.randomUUID(),
            url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos"),
            signature = "old_signature"
    )

    @Nested
    @DisplayName("should find items")
    @ExtendWith(MockServer::class)
    inner class ShouldFindItems {

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(
                    get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.with-no-items.json")))
            )
            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @Test
        fun `with no type and all items`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(50)
                    .verifyComplete()
        }

        @Test
        fun `with no type and no videos and all items`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"))
            backend.stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(p))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(50)
                    .verifyComplete()
        }

        @Test
        fun `with replay and all items`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"))
            backend.stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22replay%22%5D%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(p))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(50)
                    .verifyComplete()
        }

        @Test
        fun `with extract and all items`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/extract"))
            backend.stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22extract%22%5D%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(p))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(50)
                    .verifyComplete()
        }

        @Test
        fun `with bonus and all items`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/bonus"))
            backend.stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22bonus%22%5D%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(p))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(50)
                    .verifyComplete()
        }

        @Test
        fun `and check first item content`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(podcast).toMono())
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.title).isEqualTo("Le meilleur de la semaine du 15 juin 2019")

                        assertThat(it.description).contains("""QUOTIDIEN, présenté par Yann BARTHES avec son équipe de journalistes et de chroniqueurs, revient pour une saison 3 sur TMC.""")
                        assertThat(it.description).contains("""Le Talk-show d’actu et de culture, produit par Bangumi, continue de décrypter toutes les images qui inondent l’époque avec une formule enrichie et une équipe de reporters et d'humoristes élargie.""")
                        assertThat(it.description).contains("""Superstars internationales, invités d’actualité, nouveaux visages français, reportages à l’étranger et dans les régions, analyses, lives, humour, tous les ingrédients qui font le succès de QUOTIDIEN sont au rendez-vous du lundi au vendredi à 19h20 sur TMC.""")

                        assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2019, 6, 15, 9, 52, 44, 0, ZoneId.of("UTC")))
                        assertThat(it.url).isEqualTo(URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/meilleur-de-semaine-15-juin-2019.html"))
                        assertThat(it.cover!!.width).isEqualTo(100)
                        assertThat(it.cover!!.height).isEqualTo(200)
                        assertThat(it.cover!!.url).isEqualTo(URI("https://fake.url.com/img.png"))
                    }
                    .verifyComplete()
        }

        @Test
        fun `but fails because of no program slug`() {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/foo"))

            /* When */
            assertThatThrownBy { updater.findItems(p) }
                    /* Then */
                    .hasMessage("Slug not found in podcast with https://www.tf1.fr/foo")
        }

    }

    @Nested
    @DisplayName("should sign")
    @ExtendWith(MockServer::class)
    inner class ShouldSign {

        @Test
        fun `with no items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                        .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.with-no-items.json")))
                )
            }

            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("")
                    .verifyComplete()
        }

        @Test
        fun `with no type and all items`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                        .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
                )
            }
            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("0d1b85d92442090ce4d7320f2176e8cf")
                    .verifyComplete()
        }

        @Test
        fun `with replay type`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"))

            backend.stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22replay%22%5D%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            /* When */
            StepVerifier.create(updater.signatureOf(p.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("0d1b85d92442090ce4d7320f2176e8cf")
                    .verifyComplete()
        }

        @Test
        fun `with extract type`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/extract"))

            backend.apply {
                stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22extract%22%5D%7D""")
                        .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
                )
            }
            /* When */
            StepVerifier.create(updater.signatureOf(p.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("0d1b85d92442090ce4d7320f2176e8cf")
                    .verifyComplete()
        }

        @Test
        fun `with bonus type`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/bonus"))

            backend.apply {
                stubFor(get("""/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22bonus%22%5D%7D""")
                        .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
                )
            }
            /* When */
            StepVerifier.create(updater.signatureOf(p.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("0d1b85d92442090ce4d7320f2176e8cf")
                    .verifyComplete()
        }

        @Test
        fun `but fails because of no program slug`() {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/foo"))

            /* When */
            assertThatThrownBy { updater.signatureOf(p.url) }
                    /* Then */
                    .hasMessage("Slug not found in podcast with https://www.tf1.fr/foo")
        }

    }

    @Test
    fun `should return MyTf1 type`() {
        /* Given */
        /* When */
        val type = updater.type()
        /* Then */
        assertThat(type.key).isEqualTo("MyTF1")
        assertThat(type.name).isEqualTo("MyTF1")
    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

        @Test
        fun `should be compatible`() {
            /* Given */
            val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"
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

