package com.github.davinkevin.podcastserver.manager.worker.tf1replay

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.IOUtils.fileAsJson
import com.github.davinkevin.podcastserver.IOUtils.stringAsHtml
import com.github.davinkevin.podcastserver.IOUtils.stringAsJson
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

/**
 * Created by kevin on 21/07/2016
 */
@ExtendWith(MockitoExtension::class)
class TF1ReplayUpdaterTest {

    @Mock lateinit var signatureService: SignatureService
    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var imageService: ImageService
    @Mock lateinit var jsonService: JsonService
    @InjectMocks lateinit var updater: TF1ReplayUpdater

    @Test
    fun `should sign for replay`() {
        /* Given */
        val podcast = Podcast().apply { url = "http://www.tf1.fr/tf1/19h-live/videos" }
        whenever(jsonService.parseUrl("http://www.tf1.fr/ajax/tf1/19h-live/videos?filter=replay"))
                .then { fileAsJson("/remote/podcast/tf1replay/19h-live.ajax.replay.json") }
        whenever(htmlService.parse(any())).then { stringAsHtml(it.getArgument(0)) }
        whenever(signatureService.fromText(any())).thenCallRealMethod()

        /* When */
        val signature = updater.signatureOf(podcast)

        /* Then */
        assertThat(signature).isEqualTo("7f83bdad4764c28504e39bee7ba7d737")
    }

    @Test
    fun `should sign for standard instead of replay`() {
        /* Given */
        val podcast = Podcast().apply { url = "http://www.tf1.fr/xtra/olive-et-tom/videos" }
        doAnswer { fileAsJson("/remote/podcast/tf1replay/olive-et-tom.ajax.replay.json") }
                .whenever(jsonService).parseUrl("http://www.tf1.fr/ajax/xtra/olive-et-tom/videos?filter=replay")
        doAnswer { fileAsJson("/remote/podcast/tf1replay/olive-et-tom.ajax.json") }
                .whenever(jsonService).parseUrl("http://www.tf1.fr/ajax/xtra/olive-et-tom/videos?filter=all")
        whenever(htmlService.parse(any())).then { stringAsHtml(it.getArgument(0)) }
        whenever(signatureService.fromText(any())).thenCallRealMethod()

        /* When */
        val signature = updater.signatureOf(podcast)

        /* Then */
        assertThat(signature).isEqualTo("acf0b3a84ae2244194c67078c95a4efe")
    }

    @Test
    fun `should not get name from url`() {
        /* Given */
        val podcast = Podcast.builder().url("http://www.tf1.fr/foo/bar").build()

        /* When */
        val signature = updater.signatureOf(podcast)

        /* Then */
        assertThat(signature).isEmpty()
    }

    @Nested
    @DisplayName("should get items")
    inner class ShouldGetItems {

        val podcast = Podcast().apply { url = "https://www.tf1.fr/tf1/barbapapa/videos" }

        @BeforeEach
        fun beforeEach() {
            whenever(htmlService.parse(any())).then { stringAsHtml(it.getArgument(0)) }
            whenever(jsonService.parseUrl("http://www.tf1.fr/ajax/tf1/barbapapa/videos?filter=replay"))
                    .then { fileAsJson(from("barbapapa.replay.json")) }
            whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }
        }

        @Test
        fun standard() {
            /* Given */
            whenever(htmlService.get(any())).then { fileAsHtml(from(it.getArgument<String>(0).substringAfterLast("/"))) }

            /* When */
            val items = updater.getItems(podcast)

            /* Then */
            assertThat(items).hasSize(3)
            assertThat(items).allSatisfy {
                assertThat(it.pubDate).isBefore(ZonedDateTime.of(2018, 9, 16, 23, 59, 0, 0, ZoneId.systemDefault()))
            }
            verify(imageService, atLeast(1)).getCoverFromURL(any())
            verify(htmlService, times(3)).get(any())
        }

        @Test
        fun `with error on date`() {
            /* Given */
            doAnswer { None.toVΛVΓ() }.whenever(htmlService).get(tf1VideoOf("port-barbapapa-8.html"))
            doAnswer { fileAsHtml(from("l-amerique-barbapapa-8.without-script.html")) }
                    .whenever(htmlService).get(tf1VideoOf("l-amerique-barbapapa-8.html"))
            doAnswer { fileAsHtml(from("barbamama-barbapapa-8.without-date.html")) }
                    .whenever(htmlService).get(tf1VideoOf("barbamama-barbapapa-8.html"))

            /* When */
            val items = updater.getItems(podcast)

            /* Then */
            assertThat(items).hasSize(3)
            assertThat(items).allSatisfy {
                assertThat(it.pubDate).isAfter(now().minusMinutes(10))
            }
            verify(imageService, atLeast(1)).getCoverFromURL(any())
        }

        fun tf1VideoOf(s: String) = "https://www.tf1.fr/tf1/barbapapa/videos/$s"


    }

    @Test
    fun `should be of type`() {
        assertThat(updater.type().key).isEqualTo("TF1Replay")
        assertThat(updater.type().name).isEqualTo("TF1 Replay")
    }

    @Test
    fun `should be compatible`() {
        /* Given */
        val url = "www.tf1.fr/tf1/19h-live/videos"
        /* When */
        val compatibility = updater.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* Given */
        val url = "www.tf1.com/foo/bar/videos"
        /* When */
        val compatibility = updater.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }

    companion object {
        fun from(s: String) = "/remote/podcast/tf1replay/$s"
    }
}
