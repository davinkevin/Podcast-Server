package com.github.davinkevin.podcastserver.manager.worker.dailymotion

import arrow.core.None
import com.github.davinkevin.podcastserver.fileAsHtml
import com.github.davinkevin.podcastserver.fileAsJson
import com.github.davinkevin.podcastserver.stringAsJson
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.M3U8Service
import com.nhaarman.mockitokotlin2.*
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URI
import java.util.*

/**
 * Created by kevin on 24/12/2017
 */
@ExtendWith(MockitoExtension::class)
class DailymotionExtractorTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var jsonService: JsonService
    @Mock lateinit var m3U8Service: M3U8Service
    @InjectMocks lateinit var extractor: DailymotionExtractor

    private val item: DownloadingItem = DownloadingItem (
            id = UUID.randomUUID(),
            title = "CHROMA S01.11 LES AFFRANCHIS",
            status = Status.NOT_DOWNLOADED,
            url = URI("https://www.dailymotion.com/video/x5ikng3?param=1"),
            numberOfFail = 0,
            progression = 0,
            podcast = DownloadingItem.Podcast(
                    id = UUID.randomUUID(),
                    title = "chroma"
            ),
            cover = DownloadingItem.Cover(
                    id = UUID.randomUUID(),
                    url = URI("https://dailymotion/chroma/cover.jpg")
            )
    )

    @Test
    fun `should load chromecast stream`() {
        /* Given */
        val chromecastUrl = "https://www.dailymotion.com/cdn/manifest/video/x5ikng3.m3u8?auth=1539545073-2562-nei1ulu3-81a790d43c8e11ced7a896781f49c941"
        whenever(htmlService.get(item.url.toASCIIString()))
                .then { fileAsHtml(from("karimdebbache.chroma.s01e11.html")) }
        whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
        whenever(jsonService.parseUrl("https://www.dailymotion.com/player/metadata/video/x5ikng3?embedder=https%3A%2F%2Fwww.dailymotion.com%2Fvideo%2Fx5ikng3&locale=en&integration=inline&GK_PV5_NEON=1"))
                .then { fileAsJson(from("karimdebbache.chroma.s01e11.json")) }
        whenever(m3U8Service.getM3U8UrlFormMultiStreamFile(chromecastUrl))
                .then { "https://proxy-005.dc3.dailymotion.com/sec(c261ed40cc95bcf93923ccd7f1c92a83)/video/574/494/277494475_mp4_h264_aac_fhd.m3u8#cell=core&comment=QOEABR17&hls_maxMaxBufferLength=105" }

        /* When */
        val downloadingItem = extractor.extract(item)

        /* Then */
        assertThat(downloadingItem.urls).containsOnly("https://proxy-005.dc3.dailymotion.com/sec(c261ed40cc95bcf93923ccd7f1c92a83)/video/574/494/277494475_mp4_h264_aac_fhd.m3u8")
        assertThat(downloadingItem.item).isSameAs(item)
        assertThat(downloadingItem.filename).isEqualTo("x5ikng3.mp4")
        verify(jsonService, times(1)).parse(any())
        verify(m3U8Service, times(1)).getM3U8UrlFormMultiStreamFile(chromecastUrl)
    }

    @Test
    fun `should throw error if no result found from html remote call`() {
        /* GIVEN */
        whenever(htmlService.get(item.url.toASCIIString())).then { None }

        /* When */
        assertThatThrownBy { extractor.extract(item) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Error during Dailymotion extraction of CHROMA S01.11 LES AFFRANCHIS with url https://www.dailymotion.com/video/x5ikng3?param=1")

        /* Then */
        verify(jsonService, never()).parse(any())
        verify(m3U8Service, never()).getM3U8UrlFormMultiStreamFile(any())
    }

    @Test
    fun `should throw error if no script tag with json data`() {
        /* GIVEN */
        whenever(htmlService.get(item.url.toASCIIString()))
                .then { fileAsHtml(from("karimdebbache.chroma.s01e11.incoherent.html")) }

        /* When */
        assertThatThrownBy { extractor.extract(item) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Structure of Dailymotion page changed")

        /* Then */
        verify(jsonService, never()).parse(any())
        verify(m3U8Service, never()).getM3U8UrlFormMultiStreamFile(any())
    }

    @Test
    fun `should throw error if nothing found from json call`() {
        /* GIVEN */
        whenever(htmlService.get(item.url.toASCIIString()))
                .then { fileAsHtml(from("karimdebbache.chroma.s01e11.html")) }
        whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
        whenever(jsonService.parseUrl("https://www.dailymotion.com/player/metadata/video/x5ikng3?embedder=https%3A%2F%2Fwww.dailymotion.com%2Fvideo%2Fx5ikng3&locale=en&integration=inline&GK_PV5_NEON=1"))
                .then { None }

        /* When */
        assertThatThrownBy { extractor.extract(item) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Error during Dailymotion extraction of CHROMA S01.11 LES AFFRANCHIS with url https://www.dailymotion.com/video/x5ikng3?param=1")

        /* Then */
        verify(m3U8Service, never()).getM3U8UrlFormMultiStreamFile(any())
    }

    @Test
    fun `should not be compatible`() {
        assertThat(extractor.compatibility(URI("http://a.fake.url/with/file.mp4?param=1"))).isEqualTo(Integer.MAX_VALUE)
    }

    @Test
    fun `should be compatible`() {
        assertThat(extractor.compatibility(URI("https://dailymotion.com/video/foo/bar"))).isEqualTo(1)
    }

    private fun from(filename: String) = "/remote/podcast/dailymotion/$filename"

}
