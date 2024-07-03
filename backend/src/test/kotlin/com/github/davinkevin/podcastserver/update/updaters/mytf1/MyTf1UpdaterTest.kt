package com.github.davinkevin.podcastserver.update.updaters.mytf1

import com.github.davinkevin.podcastserver.MockServer
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.fileAsString
import com.github.davinkevin.podcastserver.remapRestClientToMockServer
import com.github.davinkevin.podcastserver.service.image.CoverInformation
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
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by kevin on 11/03/2020
 */
@ExtendWith(SpringExtension::class)
class MyTf1UpdaterTest(
    @Autowired private val updater: MyTf1Updater
) {

    @TestConfiguration
    @Import(
        RestClientAutoConfiguration::class,
        MyTf1UpdaterConfig::class,
        JacksonAutoConfiguration::class
    )
    class LocalTestConfiguration {
        @Bean fun remapToLocalHost() = remapRestClientToMockServer("www.tf1.fr")
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
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `with no items because page is empty`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(
                get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                    .willReturn(ok())
            )

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `with no type and all items`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(100)
        }

        @Test
        fun `with no covers for all items`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root-with-no-cover.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(Mono.empty())

            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(100)
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
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(100)
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
            val items = updater.findItems(p)

            /* Then */
            assertThat(items).hasSize(100)
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
            val items = updater.findItems(p)

            /* Then */
            assertThat(items).hasSize(100)
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
            val items = updater.findItems(p)

            /* Then */
            assertThat(items).hasSize(100)
        }

        @Test
        fun `and check first item content`(backend: WireMockServer) {
            /* Given */
            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )
            whenever(imageService.fetchCoverInformation(any())).thenReturn(CoverInformation(100, 200, URI("https://fake.url.com/img.png")).toMono())


            /* When */
            val items = updater.findItems(podcast)

            /* Then */
            assertThat(items).hasSize(100)
            val item = items.first()
            assertAll {
                assertThat(item.title).isEqualTo("Réforme des retraites : nouvelle journée hallucinante à l’Assemblée")

                assertThat(item.description).contains("""Après une première semaine de débats chaotique, explosive et houleuse, on espérait un retour au calme dans l’hémicycle ce lundi pour la seconde semaine de débat sur la réforme des retraites. Il n’en a rien été. On en parle avec Sophie Dupont.""")

                assertThat(item.pubDate).isEqualTo(ZonedDateTime.of(2023, 2, 13, 19, 28, 0, 0, ZoneId.of("UTC")))
                assertThat(item.url).isEqualTo(URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/reforme-des-retraites-nouvelle-journee-hallucinante-a-lassemblee-54055173.html"))
                assertThat(item.cover!!.width).isEqualTo(100)
                assertThat(item.cover!!.height).isEqualTo(200)
                assertThat(item.cover!!.url).isEqualTo(URI("https://fake.url.com/img.png"))
            }
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
            val sign = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(sign).isEqualTo("")
        }

        @Test
        fun `with no item because all page is empty`(backend: WireMockServer) {
            /* Given */
            backend.apply {
                stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D""")
                    .willReturn(ok())
                )
            }

            /* When */
            val sign = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(sign).isEqualTo("")
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
            val sign = updater.signatureOf(podcast.url)

            /* Then */
            assertThat(sign).isEqualTo("e254efc2cd90286877d9d38f45b1d5fb")
        }

        @Test
        fun `with replay type`(backend: WireMockServer) {
            /* Given */
            val url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay")

            backend.stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22REPLAY%22%5D%7D""")
                .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
            )

            /* When */
            val sign = updater.signatureOf(url)

            /* Then */
            assertThat(sign).isEqualTo("e254efc2cd90286877d9d38f45b1d5fb")
        }

        @Test
        fun `with extract type`(backend: WireMockServer) {
            /* Given */
            val url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/extract")

            backend.apply {
                stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22EXTRACT%22%5D%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
                )
            }

            /* When */
            val sign = updater.signatureOf(url)

            /* Then */
            assertThat(sign).isEqualTo("e254efc2cd90286877d9d38f45b1d5fb")
        }

        @Test
        fun `with bonus type`(backend: WireMockServer) {
            /* Given */
            val url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/bonus")

            backend.apply {
                stubFor(get("""/graphql/web?id=87a97a3&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22BONUS%22%5D%7D""")
                    .willReturn(okJson(fileAsString("/remote/podcast/mytf1/quotidien.query.root.json")))
                )
            }
            /* When */
            val sign = updater.signatureOf(url)

            /* Then */
            assertThat(sign).isEqualTo("e254efc2cd90286877d9d38f45b1d5fb")
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

