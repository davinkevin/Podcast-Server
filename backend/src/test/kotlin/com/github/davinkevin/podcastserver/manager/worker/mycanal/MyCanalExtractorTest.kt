package com.github.davinkevin.podcastserver.manager.worker.mycanal

import arrow.core.None
import com.github.davinkevin.podcastserver.fileAsHtml
import com.github.davinkevin.podcastserver.fileAsJson
import com.github.davinkevin.podcastserver.stringAsJson
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.service.HtmlService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URI
import java.util.*

/**
 * Created by kevin on 25/12/2017
 */
@ExtendWith(MockitoExtension::class)
class MyCanalExtractorTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var jsonService: JsonService
    @InjectMocks lateinit var extractor: MyCanalExtractor

    private val item: DownloadingItem = DownloadingItem (
            id = UUID.randomUUID(),
            title = "aTitle",
            status = Status.NOT_DOWNLOADED,
            url = URI("https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195"),
            numberOfFail = 0,
            progression = 0,
            podcast = DownloadingItem.Podcast(
                    id = UUID.randomUUID(),
                    title = "a canalplus podcast"
            ),
            cover = DownloadingItem.Cover(
                    id = UUID.randomUUID(),
                    url = URI("https://mycanal.com/podcast/cover.jpg")
            )
    )

    @Test
    fun should_extract() {
        /* GIVEN */
        whenever(htmlService.get("https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195")).thenReturn(fileAsHtml(from("1474195.html")))
        whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
        whenever(jsonService.parseUrl(any())).then { fileAsJson(withId(it)) }
        /* WHEN  */
        val downloadingItem = extractor.extract(item)
        /* THEN  */
        assertThat(downloadingItem.item).isSameAs(item)
        assertThat(downloadingItem.url()).isEqualTo("https://strcpluscplus-vh.akamaihd.net/i/1712/16/1191740_16_,200k,400k,800k,1500k,.mp4.csmil/master.m3u8")
        assertThat(downloadingItem.filename).isEqualTo("1474195.mp4")
    }

    @Test
    fun `should throw error if no result for html request`() {
        /* Given */
        whenever(htmlService.get("https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195")).thenReturn(None)
        /* When */
        assertThatThrownBy { extractor.extract(item) }
        /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Error during extraction of aTitle at url https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195")
    }

    private fun withId(i: InvocationOnMock) =
            from(i.getArgument<String>(0)
                    .replace("https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/", "")
                    .replace("?format=json", "") + ".json")


    private fun from(name: String) = "/remote/podcast/mycanal/$name"

    @Test
    fun should_be_compatible() {
        assertThat(extractor.compatibility(URI("https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195"))).isEqualTo(1)
    }

    @Test
    fun should_not_be_compatible() {
        assertThat(extractor.compatibility(URI("http://www.foo.fr/bar/to.html"))).isGreaterThan(1)
    }

}
