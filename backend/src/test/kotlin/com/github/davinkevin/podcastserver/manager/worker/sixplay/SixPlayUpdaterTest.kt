package com.github.davinkevin.podcastserver.manager.worker.sixplay

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils
import com.github.davinkevin.podcastserver.manager.worker.ItemFromUpdate
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Condition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URI
import java.time.ZonedDateTime
import java.util.*
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

    private val show = PodcastToUpdate (
            id = UUID.randomUUID(),
            url = URI("http://www.6play.fr/custom-show"),
            signature = "noSign"
    )

    @Test
    fun `should extract items`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/sport-6-p_1380.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(imageService.fetchCoverInformation(any<String>())).then { CoverInformation(
                url = URI(it.getArgument(0)),
                width = 200,
                height = 200
        ) }

        /* When */
        val items = updater.blockingFindItems(show)

        /* Then */
        assertThat(items).hasSize(2)
                .are(withTitle)
                .are(withCover)
                .are(withLength)
                .are(withPubDate)
    }

    @Test
    fun `should return empty if no response from http request`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(None.toVΛVΓ())
        /* When */
        val items = updater.blockingFindItems(show)
        /* Then */
        assertThat(items).isEmpty()
    }

    @Test
    fun `should throw exception if problem during get items`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/sport-6-p_1380.html"))
        whenever(jsonService.parse(any())).thenThrow(RuntimeException("Foo Bar"))

        /* When */
        assertThatThrownBy { updater.blockingFindItems(show) }

                /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Foo Bar")
    }

    @Test
    fun `should throw exception if no js found in the page`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/sport-6-p_1380-without-js.html"))

        /* When */
        assertThatThrownBy { updater.blockingFindItems(show) }

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
        assertThatThrownBy { updater.blockingFindItems(show) }

                /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("No results for path: \$['program']['programsById']")
    }

    @Test
    fun `should extract items with specific case`() {
        /* Given */
        val now = ZonedDateTime.now()
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/sport-6-p_1380-with-specific-item.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(imageService.fetchCoverInformation(any<String>())).then { CoverInformation(
                url = URI(it.getArgument(0)),
                width = 200,
                height = 200
        ) }

        /* When */
        val items = updater.blockingFindItems(show)

        /* Then */
        assertThat(items).hasSize(2)
        val i = items.first { it.url == URI("http://www.6play.fr/sport-6-p_1380/emission-du-12-aout-a-2005") }
        assertThat(i.pubDate)
                .isAfterOrEqualTo(now)
                .isBeforeOrEqualTo(ZonedDateTime.now())

    }

    @Test
    fun `should do signature`() {
        /* GIVEN */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/sport-6-p_1380.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(signatureService.fromText(any())).thenCallRealMethod()

        /* WHEN  */
        val signature = updater.blockingSignatureOf(show.url)

        /* THEN  */
        assertThat(signature).isNotEmpty()
    }

    @Test
    fun `should throw error if signature can't be done`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(None.toVΛVΓ())

        /* When */
        assertThatThrownBy { updater.blockingSignatureOf(show.url) }

                /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Error during signature of podcast with url http://www.6play.fr/custom-show")
    }

    @Test
    fun `should throw parsing exception if problem during signature`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/sport-6-p_1380.html"))
        whenever(jsonService.parse(any())).thenThrow(RuntimeException("Foo Bar"))

        /* When */
        assertThatThrownBy { updater.blockingSignatureOf(show.url) }

                /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Foo Bar")
    }

    @Test
    fun `should have the same signature twice`() {
        /* GIVEN */
        whenever(htmlService.get(any())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/sport-6-p_1380.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(signatureService.fromText(any())).thenCallRealMethod()

        /* WHEN  */
        val s1 = updater.blockingSignatureOf(show.url)
        val s2 = updater.blockingSignatureOf(show.url)

        /* THEN  */
        assertThat(s1).isEqualToIgnoringCase(s2)
    }

    @Test
    fun `should have type`() {
        assertThat(updater.type().key).isEqualTo("SixPlay")
        assertThat(updater.type().name).isEqualTo("6Play")
    }

    @Test
    fun `should be only compatible with 6play url`() {
        assertThat(updater.compatibility(null)).isGreaterThan(1)
        assertThat(updater.compatibility("foo")).isGreaterThan(1)
        assertThat(updater.compatibility("http://www.6play.fr/test")).isEqualTo(1)
    }

    private val withTitle = Condition<ItemFromUpdate> ( Predicate { it.title!!.isNotEmpty() }, "should have title not empty")
    private val withPubDate = Condition<ItemFromUpdate> ( Predicate { it.pubDate != null }, "should have pubDate not null")
    private val withLength = Condition<ItemFromUpdate> ( Predicate { it.length != null }, "should have length not null")
    private val withCover = Condition<ItemFromUpdate> ( Predicate { it.cover != null }, "should have cover not null")
}
