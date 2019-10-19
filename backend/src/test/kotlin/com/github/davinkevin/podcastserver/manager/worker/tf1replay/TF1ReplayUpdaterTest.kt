package com.github.davinkevin.podcastserver.manager.worker.tf1replay

import arrow.core.None
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.IOUtils.fileAsJson
import com.github.davinkevin.podcastserver.IOUtils.stringAsJson
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.service.JsonService
import org.apache.commons.codec.digest.DigestUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI
import java.util.*

/**
 * Created by kevin on 21/07/2016
 */

@ExtendWith(SpringExtension::class)
class TF1ReplayUpdaterTest(
    @Autowired val signatureService: SignatureService,
    @Autowired val imageService: ImageService,
    @Autowired val jsonService: JsonService,
    @Autowired val updater: TF1ReplayUpdater
) {

    @Nested
    @DisplayName("should sign")
    inner class ShouldSign {

        @Nested
        @DisplayName("with success")
        inner class WithSuccess {

            @Test
            fun `for root`() {
                /* Given */
                val podcast = PodcastToUpdate ( url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"), id = UUID.randomUUID(), signature = "noSign" )
                whenever(jsonService.parseUrl("https://www.tf1.fr/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D"))
                        .then { fileAsJson("/remote/podcast/tf1replay/quotidien.query.root.json") }
                whenever(signatureService.fromText(any())).thenCallRealMethod()

                /* When */
                val signature = updater.blockingSignatureOf(podcast.url)

                /* Then */
                assertThat(signature).isEqualTo("0d1b85d92442090ce4d7320f2176e8cf")
            }

            @Test
            fun `for videos url`() {
                /* Given */
                val podcast = PodcastToUpdate ( url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos"), id = UUID.randomUUID(), signature = "noSign" )
                whenever(jsonService.parseUrl("https://www.tf1.fr/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D"))
                        .then { fileAsJson("/remote/podcast/tf1replay/quotidien.query.root.json") }
                whenever(signatureService.fromText(any())).thenCallRealMethod()

                /* When */
                val signature = updater.blockingSignatureOf(podcast.url)

                /* Then */
                assertThat(signature).isEqualTo("0d1b85d92442090ce4d7320f2176e8cf")
            }

            @Test
            fun `for replay url`() {
                /* Given */
                val podcast = PodcastToUpdate ( url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"), id = UUID.randomUUID(), signature = "noSign" )
                whenever(jsonService.parseUrl("https://www.tf1.fr/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22replay%22%5D%7D"))
                        .then { fileAsJson("/remote/podcast/tf1replay/quotidien.query.replay.json") }
                whenever(signatureService.fromText(any())).thenCallRealMethod()

                /* When */
                val signature = updater.blockingSignatureOf(podcast.url)

                /* Then */
                assertThat(signature).isEqualTo("ff820660b80d0f315685de6a519830c4")
            }

            @Test
            fun `for extract url`() {
                /* Given */
                val podcast = PodcastToUpdate ( url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/extract"), id = UUID.randomUUID(), signature = "noSign" )
                whenever(jsonService.parseUrl("https://www.tf1.fr/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22extract%22%5D%7D"))
                        .then { fileAsJson("/remote/podcast/tf1replay/quotidien.query.extract.json") }
                whenever(signatureService.fromText(any())).thenCallRealMethod()

                /* When */
                val signature = updater.blockingSignatureOf(podcast.url)

                /* Then */
                assertThat(signature).isEqualTo("841e49d6c64251982a93eba1a291ace9")
            }

            @Test
            fun `for bonus url`() {
                /* Given */
                val podcast = PodcastToUpdate ( url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/bonus"), id = UUID.randomUUID(), signature = "noSign" )
                whenever(jsonService.parseUrl("https://www.tf1.fr/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22bonus%22%5D%7D"))
                        .then { fileAsJson("/remote/podcast/tf1replay/quotidien.query.bonus.json") }
                whenever(signatureService.fromText(any())).thenCallRealMethod()

                /* When */
                val signature = updater.blockingSignatureOf(podcast.url)

                /* Then */
                assertThat(signature).isEqualTo("90df9fa6e2aae2e66d5043142f8a90ee")
            }
        }

        @Nested
        @DisplayName("with error")
        inner class WithError {

            @Test
            fun `due to no response from json call`() {
                /* Given */
                whenever(jsonService.parseUrl(any())).thenReturn(None.toVΛVΓ())
                val podcast = PodcastToUpdate (
                        url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/bonus"),
                        signature = "noSign",
                        id = UUID.randomUUID()
                )

                /* When */
                assertThatThrownBy { updater.blockingSignatureOf(podcast.url) }
                        /* Then */
                        .isInstanceOf(RuntimeException::class.java)
                        .hasMessage("Error during signature of podcast with url https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/bonus")
            }

            @Test
            fun `due to slug not found in url`() {
                /* Given */
                val podcast = PodcastToUpdate (
                        url = URI("http://www.tf1.fr/ieafjoefjeaoijfoejifaa"),
                        id = UUID.randomUUID(),
                        signature = "noSign"
                )

                /* When */
                assertThatThrownBy { updater.blockingSignatureOf(podcast.url) }
                        /* Then */
                        .isInstanceOf(RuntimeException::class.java)
                        .hasMessage("Slug not found in podcast with http://www.tf1.fr/ieafjoefjeaoijfoejifaa")
            }
        }


    }

    @Nested
    @DisplayName("should check compatibility")
    inner class ShouldCheckCompatibility {

        @Test
        fun `with a compatible podcast`() {
            /* Given */
            val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(1)
        }

        @Test
        fun `with a NOT compatible podcast`() {
            /* Given */
            val url = "www.tf1.com/foo/bar/videos"
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }

    }

    @Nested
    @DisplayName("should get items")
    inner class ShouldGetItems {

        @Nested
        @DisplayName("with success")
        inner class WithSuccess {


            //            @BeforeEach
            fun beforeEach() {
                whenever(jsonService.parseUrl("http://www.tf1.fr/ajax/tf1/barbapapa/videos?filter=replay"))
                        .then { fileAsJson(from("barbapapa.replay.json")) }
                whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
            }

            @Test
            fun `with root url`() {
                /* Given */
                val podcast = PodcastToUpdate ( url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes"), id = UUID.randomUUID(), signature = "noSign" )
                whenever(jsonService.parseUrl("https://www.tf1.fr/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D"))
                        .then { fileAsJson("/remote/podcast/tf1replay/quotidien.query.root.json") }

                /* When */
                val items = updater.blockingFindItems(podcast)

                /* Then */
                assertThat(items).hasSize(50)
                assertThat(DigestUtils.md5Hex(items.sortedBy { it.url }.joinToString { it.url.toASCIIString() } ))
                        .isEqualTo("054a119583f45ccfd7252652ed4a5e1b")

                verify(imageService, atLeast(1)).fetchCoverInformation(any<String>())
            }

            @Test
            fun `for videos url`() {
                /* Given */
                val podcast = PodcastToUpdate ( url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos"), id = UUID.randomUUID(), signature = "noSign" )
                whenever(jsonService.parseUrl("https://www.tf1.fr/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3Anull%7D"))
                        .then { fileAsJson("/remote/podcast/tf1replay/quotidien.query.root.json") }

                /* When */
                val items = updater.blockingFindItems(podcast)

                /* Then */
                assertThat(items).hasSize(50)
                assertThat(DigestUtils.md5Hex(items.sortedBy { it.url }.joinToString { it.url.toASCIIString() }))
                        .isEqualTo("054a119583f45ccfd7252652ed4a5e1b")

                verify(imageService, atLeast(1)).fetchCoverInformation(any<String>())
            }

            @Test
            fun `for replay url`() {
                /* Given */
                val podcast = PodcastToUpdate ( url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"), id = UUID.randomUUID(), signature = "noSign" )
                whenever(jsonService.parseUrl("https://www.tf1.fr/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22replay%22%5D%7D"))
                        .then { fileAsJson("/remote/podcast/tf1replay/quotidien.query.replay.json") }

                /* When */
                val items = updater.blockingFindItems(podcast)

                /* Then */
                assertThat(items).hasSize(50)
                assertThat(DigestUtils.md5Hex(items.sortedBy { it.url }.joinToString { it.url.toASCIIString() }))
                        .isEqualTo("9b96b965b24bfed3a0a7df7e9e9f1d57")

                verify(imageService, atLeast(1)).fetchCoverInformation(any<String>())
            }

            @Test
            fun `for extract url`() {
                /* Given */
                val podcast = PodcastToUpdate ( url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/extract"), id = UUID.randomUUID(), signature = "noSign" )
                whenever(jsonService.parseUrl("https://www.tf1.fr/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22extract%22%5D%7D"))
                        .then { fileAsJson("/remote/podcast/tf1replay/quotidien.query.extract.json") }

                /* When */
                val items = updater.blockingFindItems(podcast)

                /* Then */
                assertThat(items).hasSize(50)
                assertThat(DigestUtils.md5Hex(items.sortedBy { it.url }.joinToString { it.url.toASCIIString() }))
                        .isEqualTo("945c72b65698da67432427200b0651be")

                verify(imageService, atLeast(1)).fetchCoverInformation(any<String>())
            }

            @Test
            fun `for bonus url`() {
                /* Given */
                val podcast = PodcastToUpdate ( url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/bonus"), id = UUID.randomUUID(), signature = "noSign" )
                whenever(jsonService.parseUrl("https://www.tf1.fr/graphql/web?id=6708f510f2af7e75114ab3c4378142b2ce25cd636ff5a1ae11f47ce7ad9c4a91&variables=%7B%22programSlug%22%3A%22quotidien-avec-yann-barthes%22%2C%22offset%22%3A0%2C%22limit%22%3A50%2C%22sort%22%3A%7B%22type%22%3A%22DATE%22%2C%22order%22%3A%22DESC%22%7D%2C%22types%22%3A%5B%22bonus%22%5D%7D"))
                        .then { fileAsJson("/remote/podcast/tf1replay/quotidien.query.bonus.json") }

                /* When */
                val items = updater.blockingFindItems(podcast)

                /* Then */
                assertThat(items).hasSize(50)
                assertThat(DigestUtils.md5Hex(items.map { it.url.toASCIIString() }.sortedBy { it }.joinToString { it.toString() }))
                        .isEqualTo("db4a54834b5cfed50ecd23c7db6f5f2e")

                verify(imageService, atLeast(1)).fetchCoverInformation(any<String>())
            }

            @Test
            fun `with no items because layout change`() {
                /* Given */
                whenever(jsonService.parseUrl(any())).thenReturn(None.toVΛVΓ())
                val podcast = PodcastToUpdate (
                        url = URI("https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/bonus"),
                        signature = "noSign",
                        id = UUID.randomUUID()
                )

                /* When */
                val items = updater.blockingFindItems(podcast)

                /* Then */
                assertThat(items).hasSize(0)
            }


        }

        @Nested
        @DisplayName("with error")
        inner class WithError {

            @Test
            fun `due to slug not found in url`() {
                /* Given */
                val podcast = PodcastToUpdate (
                        url = URI("http://www.tf1.fr/ieafjoefjeaoijfoejifaa"),
                        id = UUID.randomUUID(),
                        signature = "noSign"
                )

                /* When */
                assertThatThrownBy { updater.blockingFindItems(podcast) }
                        /* Then */
                        .isInstanceOf(RuntimeException::class.java)
                        .hasMessage("Slug not found in podcast with http://www.tf1.fr/ieafjoefjeaoijfoejifaa")
            }
        }




    }

    @Test
    fun `should be of type`() {
        assertThat(updater.type().key).isEqualTo("TF1Replay")
        assertThat(updater.type().name).isEqualTo("TF1 Replay")
    }

    companion object {
        fun from(s: String) = "/remote/podcast/tf1replay/$s"
    }

    @TestConfiguration
    @Import(TF1ReplayUpdater::class)
    class LocalTestConfiguration {
        @Bean fun signatureService() = mock<SignatureService>()
        @Bean fun jsonService() = mock<JsonService>()
        @Bean fun coverService() = mock<ImageService>()
        @Bean fun objectMapper() = ObjectMapper()
    }
}
