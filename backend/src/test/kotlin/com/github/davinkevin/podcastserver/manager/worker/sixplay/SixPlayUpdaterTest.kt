package com.github.davinkevin.podcastserver.manager.worker.sixplay

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
import com.github.davinkevin.podcastserver.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Condition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.ZonedDateTime
import java.util.function.Predicate

/**
 * Created by kevin on 22/12/2016
 */
@ExtendWith(MockitoExtension::class)
class SixPlayUpdaterTest {

    @Mock lateinit var signatureService: SignatureService
    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var jsonService: JsonService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var updater: SixPlayUpdater
    
    private val show = Podcast().apply {
            title = "Custom Show"
            url = "http://www.6play.fr/custom-show"
    }

    @Test
    fun `should extract items`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-main.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(imageService.getCoverFromURL(any())).thenReturn(Cover.DEFAULT_COVER)

        /* When */
        val items = updater.getItems(show)

        /* Then */
        assertThat(items)
                .hasSize(77)
                .are(allValid())
    }

    @Test
    fun `should return empty if no response from http request`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(None.toVΛVΓ())
        /* When */
        val items = updater.getItems(show)
        /* Then */
        assertThat(items).isEmpty()
    }

    @Test
    fun `should throw exception if problem during get items`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-main.html"))
        whenever(jsonService.parse(any())).thenThrow(RuntimeException("Foo Bar"))

        /* When */
        assertThatThrownBy { updater.getItems(show) }

                /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Foo Bar")
    }

    @Test
    fun `should throw exception if no js found in the page`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/sport-6-p_1380-without-js.html"))

        /* When */
        assertThatThrownBy { updater.getItems(show) }

                /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("No parsable JS found in the page")
    }

    @Test
    fun `should throw exception if no programById found`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/sport-6-p_1380-without-programid.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }

        /* When */
        assertThatThrownBy { updater.getItems(show) }

                /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("programId not found in react store")
    }

    @Test
    fun `should extract items with specific case`() {
        /* Given */
        val now = ZonedDateTime.now()
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/sport-6-p_1380-with-specific-item.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(imageService.getCoverFromURL(any())).thenReturn(Cover.DEFAULT_COVER)

        /* When */
        val items = updater.getItems(show)

        /* Then */
        assertThat(items).hasSize(2)
        val i = items.toJavaList()[0]
        assertThat(i.pubDate)
                .isAfterOrEqualTo(now)
                .isBeforeOrEqualTo(ZonedDateTime.now())
        assertThat(i.url).isEqualTo("http://www.6play.fr/sport-6-p_1380/emission-du-12-aout-a-2005")

    }
    
    @Test
    fun `should do signature`() {
        /* GIVEN */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-main.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(signatureService.fromText(any())).thenCallRealMethod()

        /* WHEN  */
        val signature = updater.signatureOf(show)

        /* THEN  */
        assertThat(signature).isNotEmpty()
    }

    @Test
    fun `should throw error if signature can't be done`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(None.toVΛVΓ())

        /* When */
        assertThatThrownBy { updater.signatureOf(show) }

                /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Error during signature of podcast Custom Show")
    }

    @Test
    fun `should_throw parsing exception if problem during signature`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-main.html"))
        whenever(jsonService.parse(any())).thenThrow(RuntimeException("Foo Bar"))

        /* When */
        assertThatThrownBy { updater.signatureOf(show) }

                /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Foo Bar")
    }

    @Test
    fun `should have the same signature twice`() {
        /* GIVEN */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-main.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(signatureService.fromText(any())).thenCallRealMethod()

        /* WHEN  */
        val s1 = updater.signatureOf(show)
        val s2 = updater.signatureOf(show)

        /* THEN  */
        assertThat(s1).isEqualToIgnoringCase(s2)
    }

    @Test
    fun `should have type`() {
        assertThat(updater.type().key()).isEqualTo("SixPlay")
        assertThat(updater.type().name()).isEqualTo("6Play")
    }

    @Test
    fun `should be only compatible with 6play url`() {
        assertThat(updater.compatibility(null)).isGreaterThan(1)
        assertThat(updater.compatibility("foo")).isGreaterThan(1)
        assertThat(updater.compatibility("http://www.6play.fr/test")).isEqualTo(1)
    }

    private fun allValid(): Condition<Item> {
        val p = Predicate<Item>{ it.url != null }
                .and { it.title.isNotEmpty() }
                .and { it.pubDate != null }
                .and { it.url.isNotEmpty() }
                .and { it.length != null }
                .and { it.cover != null }

        return Condition(p, "Should have coherent fields")
    }
}
