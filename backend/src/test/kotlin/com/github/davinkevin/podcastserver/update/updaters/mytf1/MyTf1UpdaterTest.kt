package com.github.davinkevin.podcastserver.update.updaters.mytf1

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.config.WebClientConfig
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import com.github.davinkevin.podcastserver.remapToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
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
import com.github.davinkevin.podcastserver.service.image.ImageService

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
                    get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
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
            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(podcast))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(100)
                    .verifyComplete()
        }

        @Test
        fun `with no type and no videos and all items`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"))
            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(p))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(100)
                    .verifyComplete()
        }

        @Test
        fun `with replay and all items`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"))
            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22REPLAY%22%5D%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(p))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(100)
                    .verifyComplete()
        }

        @Test
        fun `with extract and all items`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/extract"))
            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22EXTRACT%22%5D%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(p))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(100)
                    .verifyComplete()
        }

        @Test
        fun `with bonus and all items`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/bonus"))
            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22BONUS%22%5D%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(p))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(100)
                    .verifyComplete()
        }

        @Test
        fun `and check first item content`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            StepVerifier.create(updater.findItems(podcast).toMono())
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.title).isEqualTo("Réforme des retraites : nouvelle journée hallucinante à l’Assemblée")

                        assertThat(it.description).contains("""Après une première semaine de débats chaotique, explosive et houleuse, on espérait un retour au calme dans l’hémicycle ce lundi pour la seconde semaine de débat sur la réforme des retraites. Il n’en a rien été. On en parle avec Sophie Dupont.""")

                        assertThat(it.pubDate).isEqualTo(ZonedDateTime.of(2023, 2, 13, 19, 28, 0, 0, ZoneId.of("UTC")))
                        assertThat(it.url).isEqualTo(URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/reforme-des-retraites-nouvelle-journee-hallucinante-a-lassemblee-54055173.html"))
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
                stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
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
                stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                        .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
                )
            }
            /* When */
            StepVerifier.create(updater.signatureOf(podcast.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("e254efc2cd90286877d9d38f45b1d5fb")
                    .verifyComplete()
        }

        @Test
        fun `with replay type`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"))

            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22REPLAY%22%5D%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            /* When */
            StepVerifier.create(updater.signatureOf(p.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("e254efc2cd90286877d9d38f45b1d5fb")
                    .verifyComplete()
        }

        @Test
        fun `with extract type`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/extract"))

            backend.apply {
                stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22EXTRACT%22%5D%7D""")
                        .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
                )
            }
            /* When */
            StepVerifier.create(updater.signatureOf(p.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("e254efc2cd90286877d9d38f45b1d5fb")
                    .verifyComplete()
        }

        @Test
        fun `with bonus type`(backend: WireMockServer) {
            /* Given */
            val p = podcast.copy(url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/bonus"))

            backend.apply {
                stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22BONUS%22%5D%7D""")
                        .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
                )
            }
            /* When */
            StepVerifier.create(updater.signatureOf(p.url))
                    /* Then */
                    .expectSubscription()
                    .expectNext("e254efc2cd90286877d9d38f45b1d5fb")
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
    }
}

