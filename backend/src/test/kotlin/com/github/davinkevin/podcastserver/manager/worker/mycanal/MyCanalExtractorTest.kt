package com.github.davinkevin.podcastserver.manager.worker.mycanal

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.IOUtils.fileAsJson
import com.github.davinkevin.podcastserver.IOUtils.stringAsJson
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
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

/**
 * Created by kevin on 25/12/2017
 */
@ExtendWith(MockitoExtension::class)
class MyCanalExtractorTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var jsonService: JsonService
    @InjectMocks lateinit var extractor: MyCanalExtractor

    private var item = Item().apply {
        title = "aTitle"
        url = "https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195"
    }

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
        assertThat(downloadingItem.url().toList()).containsOnly("https://strcpluscplus-vh.akamaihd.net/i/1712/16/1191740_16_,200k,400k,800k,1500k,.mp4.csmil/master.m3u8")
        assertThat(downloadingItem.filename).isEqualTo("1474195.mp4")
    }

    @Test
    fun `should throw error if no result for html request`() {
        /* Given */
        whenever(htmlService.get("https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195")).thenReturn(None.toVΛVΓ())
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
        assertThat(extractor.compatibility("https://www.mycanal.fr/divertissement/le-tube-du-23-12-best-of/p/1474195")).isEqualTo(1)
    }

    @Test
    fun should_not_be_compatible() {
        assertThat(extractor.compatibility("http://www.foo.fr/bar/to.html")).isGreaterThan(1)
    }

}