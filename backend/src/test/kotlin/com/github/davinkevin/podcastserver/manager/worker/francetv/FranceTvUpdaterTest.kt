package com.github.davinkevin.podcastserver.manager.worker.francetv

import arrow.core.None
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.service.JsonService
import lan.dk.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.function.Predicate
import javax.validation.Validator

/**
 * Created by kevin on 01/07/2017.
 */
@ExtendWith(MockitoExtension::class)
class FranceTvUpdaterTest {

    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Mock lateinit var signatureService: SignatureService
    @Mock lateinit var validator: Validator
    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var imageService: ImageService
    @Mock lateinit var jsonService: JsonService
    @InjectMocks lateinit var franceTvUpdater: FranceTvUpdater

    @Nested
    @DisplayName("should sign")
    inner class SignatureTest {
        @Test
        fun `the podcast`() {
            /* Given */
            whenever(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.v2.html")))
            whenever(signatureService.fromText(any())).thenCallRealMethod()

            /* When */
            val signature = franceTvUpdater.signatureOf(PODCAST)

            /* Then */
            assertThat(signature)
                    .isNotEmpty()
                    .isEqualTo("9f243961af9ff479b30c03f8054e4df9")
        }

        @Test
        fun `and have same signature even if date change`() {
            /* Given */
            whenever(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.v2_changed.html")))
            whenever(signatureService.fromText(any())).thenCallRealMethod()

            /* When */
            val signature = franceTvUpdater.signatureOf(PODCAST)

            /* Then */
            assertThat(signature)
                    .isNotEmpty()
                    .isEqualTo("9f243961af9ff479b30c03f8054e4df9")
        }

        @Test
        fun `and have different sign if new item in podcast`() {
            /* Given */
            whenever(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.v2_with_less_items.html")))
            whenever(signatureService.fromText(any())).thenCallRealMethod()

            /* When */
            val signature = franceTvUpdater.signatureOf(PODCAST)

            /* Then */
            assertThat(signature)
                    .isNotEmpty()
                    .isEqualTo("76418e0bd874d00a51d4b3b9a0b5e9ca")
                    .isNotEqualTo("9f243961af9ff479b30c03f8054e4df9")
        }

        @Test
        fun `podcast with no items`() {
            /* Given */
            whenever(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.v2_with_no_sections.html")))
            whenever(signatureService.fromText(any())).thenCallRealMethod()

            /* When */
            val signature = franceTvUpdater.signatureOf(PODCAST)

            /* Then */
            assertThat(signature)
                    .isNotEmpty()
                    .isEqualTo("d41d8cd98f00b204e9800998ecf8427e")
        }
    }

    @Nested
    @DisplayName("should get items")
    inner class GetItemTest {

        @Test
        fun `with standard format`() {
            /* Given */
            whenever(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.v2.html")))
            whenever(jsonService.parseUrl(any())).then { loadJsonCatalog(it) }
            whenever(imageService.getCoverFromURL(any())).thenReturn(Cover())

            /* When */
            val items = franceTvUpdater.getItems(PODCAST)

            /* Then */
            assertThat(items)
                    .hasSize(3)
                    .are(allValid())
        }

        @Test
        fun `even if some are not well formatted`() {
            /* Given */
            whenever(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.v2_not_well_formatted.html")))
            whenever(jsonService.parseUrl(any())).then { loadJsonCatalog(it) }
            whenever(imageService.getCoverFromURL(any())).thenReturn(Cover())

            /* When */
            val items = franceTvUpdater.getItems(PODCAST)

            /* Then */
            assertThat(items)
                    .hasSize(3)
                    .contains(Item.DEFAULT_ITEM)
        }

        @Test
        fun `and return empty list if no item found in the first ul video-list is empty`() {
            /* Given */
            whenever(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.v2_with_no_items.html")))

            /* When */
            val items = franceTvUpdater.getItems(PODCAST)

            /* Then */
            assertThat(items).hasSize(0)
        }

        @Test
        fun `and return empty list if url request failed`() {
            /* Given */
            whenever(htmlService.get(FRANCETV_URL)).thenReturn(None.toVΛVΓ())

            /* When */
            val items = franceTvUpdater.getItems(PODCAST)

            /* Then */
            assertThat(items).hasSize(0)
        }

        @Test
        fun `with specific titles`() {
            /* Given */
            whenever(htmlService.get(FRANCETV_URL)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.v2_with_items_with_title_specific.html")))
            whenever(jsonService.parseUrl(any())).then { loadJsonCatalog(it) }
            whenever(imageService.getCoverFromURL(any())).thenReturn(Cover())

            /* When */
            val items = franceTvUpdater.getItems(PODCAST)

            /* Then */
            assertThat(items)
                    .hasSize(3)
                    .filteredOn { it.title.contains("S2E3") }
                    .are(allValid())
                    .hasSize(1)
        }
    }

    @Test
    fun `should has francetv type`() {
        assertThat(franceTvUpdater.type().key()).isEqualTo("FranceTv")
        assertThat(franceTvUpdater.type().name()).isEqualTo("France•tv")
    }

    @Test
    fun `should handle compatibility`() {
        assertThat(franceTvUpdater.compatibility(null)).isEqualTo(Integer.MAX_VALUE)
        assertThat(franceTvUpdater.compatibility("http://www.france.tv/show/for/dummies")).isEqualTo(1)
        assertThat(franceTvUpdater.compatibility("http://www.f1a2.tv/show/for/dummies")).isEqualTo(Integer.MAX_VALUE)
    }

    private fun loadJsonCatalog(i: InvocationOnMock): Any {
        val url = "${i.getArgument<String>(0).removePrefix("https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=")}.json"
        return IOUtils.fileAsJson(from(url))
    }

    companion object {
        private const val FRANCETV_URL = "https://www.france.tv/france-2/secrets-d-histoire/"
        private var PODCAST = Podcast().apply { url = FRANCETV_URL }


        internal fun from(name: String): String {
            return String.format("/remote/podcast/francetv/%s", name)
        }

        private fun allValid(): Condition<Item> {
            val p = Predicate<Item>{ !it.url.isNullOrEmpty() }
                    .and { !it.title.isNullOrEmpty() }
                    .and { !it.description.isNullOrEmpty() }
                    .and { it.cover != null }
                    .and { it.pubDate != null }

            return Condition(p, "Should have coherent fields")
        }


    }

}