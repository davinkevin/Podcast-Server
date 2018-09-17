package com.github.davinkevin.podcastserver.manager.worker.tf1replay

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 21/07/2016.
 */
@ExtendWith(MockitoExtension::class)
class TF1ReplayFinderTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var finder: TF1ReplayFinder

    @Test
    fun `should fetch from html page`() {
        /* Given */
        val anUrl = "www.tf1.fr/tf1/19h-live/videos"
        whenever(htmlService.get(anUrl)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/19h-live.html"))
        whenever(imageService.getCoverFromURL(any())).then { i -> Cover().apply { url = i.getArgument(0) } }

        /* When */
        val podcast = finder.find(anUrl)

        /* Then */
        assertThat(podcast.title).isEqualTo("Vidéos & Replay 19h live - TF1")
        assertThat(podcast.description).isEqualTo("Tous les replays  19h live: les vidéos bonus exclusives des coulisses, des interviews de  19h live:")
        assertThat(podcast.url).isEqualTo(anUrl)
        assertThat(podcast.type).isEqualTo("TF1Replay")
        assertThat(podcast.cover.url).isEqualTo("http://photos1.tf1.fr/1920/960/1920x1080-19h-5619b8-0@1x.jpg")
    }

    @Test
    fun `should fetch from html page without url`() {
        /* Given */
        val anUrl = "www.tf1.fr/tf1/19h-live/videos"
        whenever(htmlService.get(anUrl)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/19h-live.withoutpicture.html"))
        whenever(imageService.getCoverFromURL(any())).then { i -> Cover().apply { url = i.getArgument(0) } }

        /* When */
        val podcast = finder.find(anUrl)

        /* Then */
        assertThat(podcast.title).isEqualTo("Vidéos & Replay 19h live - TF1")
        assertThat(podcast.description).isEqualTo("Tous les replays  19h live: les vidéos bonus exclusives des coulisses, des interviews de  19h live:")
        assertThat(podcast.url).isEqualTo(anUrl)
        assertThat(podcast.type).isEqualTo("TF1Replay")
        assertThat(podcast.cover.url).isEqualTo("http://photos2.tf1.fr/130/65/logo_programme-284-3955bf-0@1x.jpg")
    }

    @Test
    fun `should return default podcast if request end up without result`() {
        /* Given */
        val anUrl = "www.tf1.fr/tf1/19h-live/videos"
        whenever(htmlService.get(anUrl)).thenReturn(None.toVΛVΓ())

        /* When */
        val podcast = finder.find(anUrl)

        /* Then */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST)
    }

    @Test
    fun `should have default cover if nothing comming back from backend server`() {
        /* Given */
        val anUrl = "www.tf1.fr/tf1/19h-live/videos"
        whenever(htmlService.get(anUrl)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/19h-live.html"))
        whenever(imageService.getCoverFromURL(any())).thenReturn(null)

        /* When */
        val podcast = finder.find(anUrl)

        /* Then */
        assertThat(podcast.title).isEqualTo("Vidéos & Replay 19h live - TF1")
        assertThat(podcast.description).isEqualTo("Tous les replays  19h live: les vidéos bonus exclusives des coulisses, des interviews de  19h live:")
        assertThat(podcast.url).isEqualTo(anUrl)
        assertThat(podcast.type).isEqualTo("TF1Replay")
        assertThat(podcast.cover).isSameAs(Cover.DEFAULT_COVER)

    }

    @Test
    fun `should be compatible`() {
        /* Given */
        val url = "www.tf1.fr/tf1/19h-live/videos"
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* Given */
        val url = "www.tf1.com/foo/bar/videos"
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }
}
