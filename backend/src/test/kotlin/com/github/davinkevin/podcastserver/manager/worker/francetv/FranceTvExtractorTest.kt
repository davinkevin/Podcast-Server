package com.github.davinkevin.podcastserver.manager.worker.francetv

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.IOUtils.fileAsJson
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 24/12/2017
 */
@ExtendWith(MockitoExtension::class)
class FranceTvExtractorTest {

    @Mock lateinit var jsonService: JsonService
    @Mock lateinit var htmlService: HtmlService
    @InjectMocks lateinit var extractor: FranceTvExtractor

    val item = Item().apply {
        title = "Secrets d'histoire - Jeanne d'Arc, au nom de Dieu"
        url = ITEM_URL
    }

    @Test
    fun `should get url for given item`() {
        /* GIVEN */
        whenever(htmlService.get(ITEM_URL)).thenReturn(fileAsHtml(from(HTML_ITEM)))
        whenever(jsonService.parseUrl(fromCatalog(JSON_ITEM_ID))).thenReturn(fileAsJson(from("$JSON_ITEM_ID.json")))

        /* WHEN  */
        val downloadingItem = extractor.extract(item)

        /* THEN  */
        assertThat(downloadingItem.url()).containsOnly(REAL_URL)
        assertThat(downloadingItem.item).isSameAs(item)
        assertThat(downloadingItem.filename).isEqualTo("14383-secrets-d-histoire-jeanne-d-arc-au-nom-de-dieu.mp4")
        verify(htmlService, times(1)).get(ITEM_URL)
        verify(jsonService, times(1)).parseUrl(any())
    }


    @Test
    fun `should use m3u8 url as backup if no hsl stream`() {
        /* GIVEN */
        whenever(htmlService.get(ITEM_URL)).thenReturn(fileAsHtml(from(HTML_ITEM)))
        whenever(jsonService.parseUrl(fromCatalog(JSON_ITEM_ID))).thenReturn(fileAsJson(from("${JSON_ITEM_ID}_without_hls_stream.json")))

        /* WHEN  */
        val downloadingItem = extractor.extract(item)

        /* THEN  */
        assertThat(downloadingItem.url()).containsOnly("https://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif_france-dom-tom/2017/S26/J4/006a3008-8f95-52d3-be47-c15cf3640542_1498732103-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8")
        assertThat(downloadingItem.item).isSameAs(item)
        assertThat(downloadingItem.filename).isEqualTo("14383-secrets-d-histoire-jeanne-d-arc-au-nom-de-dieu.mp4")
        verify(htmlService, times(1)).get(ITEM_URL)
        verify(jsonService, times(1)).parseUrl(any())
    }

    @Test
    fun `should use first m3u8 stream if two formats are not found`() {
        /* GIVEN */
        whenever(htmlService.get(ITEM_URL)).thenReturn(fileAsHtml(from(HTML_ITEM)))
        whenever(jsonService.parseUrl(fromCatalog(JSON_ITEM_ID))).thenReturn(fileAsJson(from("${JSON_ITEM_ID}_without_hls_and_official_m3u8.json")))

        /* WHEN  */
        val downloadingItem = extractor.extract(item)

        /* THEN  */
        assertThat(downloadingItem.url()).containsOnly("https://fake.url.com/index.m3u8")
        assertThat(downloadingItem.item).isSameAs(item)
        assertThat(downloadingItem.filename).isEqualTo("14383-secrets-d-histoire-jeanne-d-arc-au-nom-de-dieu.mp4")
        verify(htmlService, times(1)).get(ITEM_URL)
        verify(jsonService, times(1)).parseUrl(any())
    }

    @Test
    fun `should use not secure url if secured not found`() {
        /* GIVEN */
        whenever(htmlService.get(ITEM_URL)).then { fileAsHtml(from(HTML_ITEM)) }
        whenever(jsonService.parseUrl(fromCatalog(JSON_ITEM_ID))).thenReturn(fileAsJson(from("${JSON_ITEM_ID}_without_secured_url.json")))

        /* WHEN  */
        val downloadingItem = extractor.extract(item)

        /* THEN  */
        assertThat(downloadingItem.url()).containsOnly("http://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif_france-dom-tom/2017/S26/J4/006a3008-8f95-52d3-be47-c15cf3640542_1498732103-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8?audiotrack=0%3Afra%3AFrancais")
        assertThat(downloadingItem.item).isSameAs(item)
        assertThat(downloadingItem.filename).isEqualTo("14383-secrets-d-histoire-jeanne-d-arc-au-nom-de-dieu.mp4")
        verify(htmlService, times(1)).get(ITEM_URL)
        verify(jsonService, times(1)).parseUrl(any())
    }

    @Test
    fun `should throw exception if no url found`() {
        /* GIVEN */
        whenever(htmlService.get(ITEM_URL)).then { fileAsHtml(from(HTML_ITEM)) }
        whenever(jsonService.parseUrl(fromCatalog(JSON_ITEM_ID))).thenReturn(fileAsJson(from("${JSON_ITEM_ID}_without_videos.json")))

        /* WHEN  */
        assertThatThrownBy { extractor.extract(item) }
                /* THEN  */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageStartingWith("No video found in this FranceTvItem")
    }

    @Test
    fun `should throw exception if can t find url at all`() {
        /* GIVEN */
        whenever(htmlService.get(ITEM_URL)).thenReturn(None.toVΛVΓ())

        /* WHEN  */
        assertThatThrownBy { extractor.extract(item) }
                /* THEN  */
                .isInstanceOf(RuntimeException::class.java)
                .withFailMessage("Url not found for " + item.url)
    }

    @Test
    fun `should be compatible`() {
        /* GIVEN */
        val url = "https://www.france.tv/foo/bar/toto"
        /* WHEN  */
        val compatibility = extractor.compatibility(url)
        /* THEN  */
        assertThat(compatibility).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* GIVEN */
        val url = "https://www.france2.tv/foo/bar/toto"
        /* WHEN  */
        val compatibility = extractor.compatibility(url)
        /* THEN  */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }

    companion object {

        private const val ITEM_URL = "https://www.france.tv/spectacles-et-culture/emissions-culturelles/14383-secrets-d-histoire-jeanne-d-arc-au-nom-de-dieu.html"
        private const val REAL_URL = "https://ftvingest-vh.akamaihd.net/i/ingest/streaming-adaptatif_france-dom-tom/2017/S26/J4/006a3008-8f95-52d3-be47-c15cf3640542_1498732103-h264-web-,398k,632k,934k,1500k,.mp4.csmil/master.m3u8?audiotrack=0%3Afra%3AFrancais"
        private const val HTML_ITEM = "200015-le-divorce-satyrique-fondateur-de-la-legende-noire-de-la-reine-margot.html"
        private const val JSON_ITEM_ID = "006a3008-8f95-52d3-be47-c15cf3640542"

        private fun from(s: String) = "/remote/podcast/francetv/$s"
        private fun fromCatalog(id: String) = "https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=$id"
    }

}