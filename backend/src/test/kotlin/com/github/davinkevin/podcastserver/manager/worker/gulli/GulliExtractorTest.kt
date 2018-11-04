package com.github.davinkevin.podcastserver.manager.worker.gulli

import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.IOUtils.stringAsJson
import com.github.davinkevin.podcastserver.service.HtmlService
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
 * Created by kevin on 11/12/2017
 */
@ExtendWith(MockitoExtension::class)
class GulliExtractorTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var jsonService: JsonService
    @InjectMocks lateinit var extractor: GulliExtractor
    
    private val item: Item = Item.builder()
            .title("Gulli Item")
            .url("http://replay.gulli.fr/")
            .build()

    @Test
    fun `should find url for gulli item`() {
        /* Given */
        whenever(htmlService.get("http://replay.gulli.fr/")).thenReturn(fileAsHtml(from("embed.VOD68526621555000.html")))
        whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }

        /* When */
        val downloadingItem = extractor.extract(item)

        /* Then */
        assertThat(downloadingItem.url().toList()).containsOnly("http://gulli-replay-mp4.scdn.arkena.com/68526621555000/68526621555000_1500.mp4")
        verify(htmlService, times(1)).get("http://replay.gulli.fr/")
        verify(jsonService, times(1)).parse(any())
    }

    @Test
    fun `should not find element if playlist item not set`() {
        /* Given */
        whenever(htmlService.get("http://replay.gulli.fr/")).thenReturn(fileAsHtml(from("embed.VOD68526621555000.without.position.html")))

        /* When */
        assertThatThrownBy { extractor.extract(item) }

                /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Gulli Url extraction failed")
    }

    @Test
    fun `should be compatible`() {
        assertThat(extractor.compatibility("http://replay.gulli.fr/dessins-animes/Pokemon3"))
                .isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        assertThat(extractor.compatibility("http://foo.bar.fr/dessins-animes/Pokemon3"))
                .isEqualTo(Integer.MAX_VALUE)
    }

    companion object {
        fun from(s: String) = "/remote/podcast/gulli/$s"
    }

}