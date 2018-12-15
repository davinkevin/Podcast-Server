package com.github.davinkevin.podcastserver.manager.worker.jeuxvideocom

import arrow.core.None
import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.manager.worker.jeuxvideocom.JeuxVideoComUpdater.Companion.JEUXVIDEOCOM_HOST
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.service.properties.PodcastServerParameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import javax.validation.Validator

/**
 * Created by kevin on 10/12/2015 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class JeuxVideoComUpdaterTest {

    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Mock lateinit var signatureService: SignatureService
    @Mock lateinit var validator: Validator
    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var updater: JeuxVideoComUpdater

    val podcast = Podcast().apply {
        title = "Chronique Video HD"
        url = "http://www.jeuxvideo.com/chroniques-video.htm"
    }

    @Test
    fun `should sign podcast`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(fileAsHtml(from("chroniques-video.htm")))
        whenever(signatureService.fromText(any())).thenCallRealMethod()

        /* When */
        val signature = updater.signatureOf(podcast)

        /* Then */
        assertThat(signature).isEqualTo("216e430c392256d2954d3903e2c8ee00")
    }

    @Test
    fun `should error during sign`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(None.toVΛVΓ())

        /* When */
        val signature = updater.signatureOf(podcast)

        /* Then */
        assertThat(signature).isEqualTo("")
    }

    @Test
    fun `should get items`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(fileAsHtml(from("chroniques-video.htm")))
        configureForAllPage("/remote/podcast/JeuxVideoCom/chroniques-video.htm")

        /* When */
        val items = updater.getItems(podcast)

        /* Then */
        assertThat(items).hasSize(42)
    }

    @Test
    fun `should return empty list if not found`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(None.toVΛVΓ())

        /* When */
        val items = updater.getItems(podcast)

        /* Then */
        assertThat(items).isEmpty()
    }

    @Test
    fun `should get items with exception`() {
        /* Given */
        whenever(htmlService.get(podcast.url)).thenReturn(fileAsHtml(from("chroniques-video.htm")))
        configureForAllPage(from("chroniques-video.htm"))
        whenever(htmlService.get("http://www.jeuxvideo.com/videos/chroniques/452234/seul-face-aux-tenebres-le-rodeur-de-la-bibliotheque.htm"))
                .thenReturn(None.toVΛVΓ())

        /* When */
        val items = updater.getItems(podcast)

        /* Then */
        assertThat(items)
                .hasSize(42)
                .contains(Item.DEFAULT_ITEM)
    }

    @Test
    fun `should be of type`() {
        assertThat(updater.type().key()).isEqualTo("JeuxVideoCom")
        assertThat(updater.type().name()).isEqualTo("JeuxVideo.com")
    }

    private fun configureForAllPage(file: String) {
        val page = fileAsHtml(file,"http://www.jeuxvideo.com")
                .k()
                .getOrElse { throw RuntimeException("Configure All page failed") }

        page.select("article")
                .flatMap { e -> e.select("a").firstOption().toList() }
                .map { it.attr("href") }
                .forEach { url ->
                    doReturn(fileAsHtml("/remote/podcast/JeuxVideoCom/" + url.substringAfterLast("/")))
                            .whenever(htmlService).get(JEUXVIDEOCOM_HOST + url)
                }
    }

    @Test
    fun `should be compatible`() {
        /* Given */
        val url = "www.jeuxvideo.com/foo/bar"

        /* When */
        val compatibility = updater.compatibility(url)

        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* Given */
        val url = "www.youtube.com/foo/bar"

        /* When */
        val compatibility = updater.compatibility(url)

        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }

    companion object {
        fun from(s: String) = "/remote/podcast/JeuxVideoCom/$s"
    }

}
