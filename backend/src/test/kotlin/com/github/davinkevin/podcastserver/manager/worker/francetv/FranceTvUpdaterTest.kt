package com.github.davinkevin.podcastserver.manager.worker.francetv

import arrow.core.None
import com.github.davinkevin.podcastserver.fileAsHtml
import com.github.davinkevin.podcastserver.fileAsJson
import com.github.davinkevin.podcastserver.manager.worker.ItemFromUpdate
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.manager.worker.defaultItem
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.stringAsJson
import com.jayway.jsonpath.DocumentContext
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.jsoup.nodes.Document
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.invocation.InvocationOnMock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI
import java.util.*
import java.util.function.Predicate
import javax.validation.Validator

/**
 * Created by kevin on 01/07/2017.
 */
@ExtendWith(SpringExtension::class)
class FranceTvUpdaterTest(
    @Autowired val signatureService: SignatureService,
    @Autowired val htmlService: HtmlService,
    @Autowired val imageService: ImageService,
    @Autowired val jsonService: JsonService,
    @Autowired val franceTvUpdater: FranceTvUpdater
) {

    private val podcast = PodcastToUpdate(
            id = UUID.randomUUID(),
            url = URI("https://www.france.tv/france-2/secrets-d-histoire"),
            signature = "old_signature"
    )

    @Nested
    @DisplayName("should sign")
    inner class SignatureTest {

        private val firstPageUrl = "https://www.france.tv/france-2/secrets-d-histoire/replay-videos/ajax/?page=0"

        @Test
        fun `the podcast`() {
            /* Given */
            whenever(htmlService.get(firstPageUrl))
                    .thenReturn(fileAsHtml(from("secrets-d-histoire.v2.html")))
            whenever(signatureService.fromText(any())).thenCallRealMethod()

            /* When */
            val signature = franceTvUpdater.blockingSignatureOf(podcast.url)

            /* Then */
            assertThat(signature)
                    .isNotEmpty()
                    .isEqualTo("fcbc57d5828e07116a2275460bf63f97")
        }

        @Test
        fun `and have same signature even if duration change`() {
            /* Given */
            whenever(htmlService.get(firstPageUrl)).thenReturn(fileAsHtml(from("secrets-d-histoire.v2_changed.html")))
            whenever(signatureService.fromText(any())).thenCallRealMethod()

            /* When */
            val signature = franceTvUpdater.blockingSignatureOf(podcast.url)

            /* Then */
            assertThat(signature)
                    .isNotEmpty()
                    .isEqualTo("fcbc57d5828e07116a2275460bf63f97")
        }

        @Test
        fun `and have different sign if podcast has less items`() {
            /* Given */
            whenever(htmlService.get(firstPageUrl)).thenReturn(fileAsHtml(from("secrets-d-histoire.v2_with_less_items.html")))
            whenever(signatureService.fromText(any())).thenCallRealMethod()

            /* When */
            val signature = franceTvUpdater.blockingSignatureOf(podcast.url)

            /* Then */
            assertThat(signature)
                    .isNotEmpty()
                    .isEqualTo("60f364a2cb0b9e7567aa1c3cac88351f")
                    .isNotEqualTo("fcbc57d5828e07116a2275460bf63f97")
        }

        @Test
        fun `podcast with no items`() {
            /* Given */
            whenever(htmlService.get(firstPageUrl)).thenReturn(fileAsHtml(from("secrets-d-histoire.v2_with_no_items.html")))
            whenever(signatureService.fromText(any())).thenCallRealMethod()

            /* When */
            val signature = franceTvUpdater.blockingSignatureOf(podcast.url)

            /* Then */
            assertThat(signature)
                    .isNotEmpty()
                    .isEqualTo("d41d8cd98f00b204e9800998ecf8427e")
        }

        @Test
        fun `and return empty list because layout may have changed`() {
            /* Given */
            whenever(htmlService.get(firstPageUrl)).thenReturn(None)

            /* When */
            val signature = franceTvUpdater.blockingSignatureOf(podcast.url)

            /* Then */
            assertThat(signature)
                    .isNotEmpty()
                    .isEqualTo("d41d8cd98f00b204e9800998ecf8427e")
        }
    }

    @Nested
    @DisplayName("should find items")
    inner class ShouldFindItems {

        private val firstPageUrl = "https://www.france.tv/france-2/secrets-d-histoire/replay-videos/ajax/?page=0"
        private val itemUrl = "https://www.france.tv/france-2/secrets-d-histoire/948775-immersion-dans-le-mystere-toutankhamon.html"

        private fun pageItemUrlToHtml(url: String): arrow.core.Option<Document> {
            val fileName = url.substringAfterLast("/")
            return fileAsHtml(from(fileName))
        }

        @Test
        fun `with standard format`() {
            /* Given */
            whenever(htmlService.get(firstPageUrl)).thenReturn(fileAsHtml(from("secrets-d-histoire.v2.html")))
            whenever(htmlService.get(argWhere { it != firstPageUrl })).then { pageItemUrlToHtml(it.getArgument(0))  }
            whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
            whenever(jsonService.parseUrl(any())).then { loadJsonCatalog(it) }
            whenever(imageService.fetchCoverInformation(any<String>())).thenReturn(CoverInformation(100, 400, URI("https://foo.bar.com/img.png")))

            /* When */
            val items = franceTvUpdater.blockingFindItems(podcast)

            /* Then */
            assertThat(items)
                    .hasSize(20)
                    .are(allValid())
        }

        @Test
        fun `and return default because the only one present doesnt have any script tag`() {
            /* Given */
            whenever(htmlService.get(firstPageUrl)).thenReturn(fileAsHtml(from("secrets-d-histoire.v2.with-one-item.html")))
            whenever(htmlService.get(itemUrl)).then { None  }

            /* When */
            val items = franceTvUpdater.blockingFindItems(podcast)

            /* Then */
            assertThat(items)
                    .hasSize(1)
                    .contains(defaultItem)
        }

        @Test
        fun `and return empty list because layout may have changed`() {
            /* Given */
            whenever(htmlService.get(firstPageUrl)).thenReturn(None)

            /* When */
            val items = franceTvUpdater.blockingFindItems(podcast)

            /* Then */
            assertThat(items)
                    .hasSize(0)
        }
    }

    @Test
    fun `should has francetv type`() {
        assertThat(franceTvUpdater.type().key).isEqualTo("FranceTv")
        assertThat(franceTvUpdater.type().name).isEqualTo("France•tv")
    }

    @Test
    fun `should handle compatibility`() {
        assertThat(franceTvUpdater.compatibility(null)).isEqualTo(Integer.MAX_VALUE)
        assertThat(franceTvUpdater.compatibility("http://www.france.tv/show/for/dummies")).isEqualTo(1)
        assertThat(franceTvUpdater.compatibility("http://www.f1a2.tv/show/for/dummies")).isEqualTo(Integer.MAX_VALUE)
    }

    private fun loadJsonCatalog(i: InvocationOnMock): arrow.core.Option<DocumentContext> {
        val url = "${i.getArgument<String>(0).removePrefix("https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=")}.json"
        return fileAsJson(from(url))
    }

    @TestConfiguration
    @Import(FranceTvUpdater::class)
    class LocalTestConfiguration {
        @Bean fun podcastServerParameters() = mock<PodcastServerParameters>()
        @Bean fun signatureService() = mock<SignatureService>()
        @Bean fun validator() = mock<Validator>()
        @Bean fun htmlService() = mock<HtmlService>()
        @Bean fun imageService() = mock<ImageService>()
        @Bean fun jsonService() = mock<JsonService>()

    }

    companion object {
        internal fun from(name: String) = "/remote/podcast/francetv/$name"
        private fun allValid(): Condition<ItemFromUpdate> {
            val p = Predicate<ItemFromUpdate>{ !it.title.isNullOrEmpty() }
                    .and { !it.description.isNullOrEmpty() }
                    .and { it.cover != null }

            return Condition(p, "Should have coherent fields")
        }
    }
}
