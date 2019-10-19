package com.github.davinkevin.podcastserver.manager.worker.tf1replay

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Created by kevin on 21/07/2016.
 */
@ExtendWith(SpringExtension::class)
class TF1ReplayFinderTest(
    @Autowired val htmlService: HtmlService,
    @Autowired val imageService: ImageService,
    @Autowired val finder: TF1ReplayFinder
) {

    @Test
    fun `should fetch from html page`() {
        /* Given */
        val anUrl = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"
        whenever(htmlService.get(anUrl)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/quotidien.root.html"))
        whenever(imageService.getCoverFromURL(any())).then { i -> Cover().apply { url = i.getArgument(0) } }

        /* When */
        val podcast = finder.find(anUrl)

        /* Then */
        assertThat(podcast.title).isEqualTo("Quotidien avec Yann Barthès - TMC | MYTF1")
        assertThat(podcast.description).isEqualTo("Yann Barthès est désormais sur TMC et TF1")
        assertThat(podcast.url).isEqualTo(anUrl)
        assertThat(podcast.type).isEqualTo("TF1Replay")
        assertThat(podcast.cover!!.url).isEqualTo("https://photos.tf1.fr/1200/0/vignette-portrait-quotidien-2-aa530a-0@1x.png")
    }

    @Test
    fun `should fetch from html page without url`() {
        /* Given */
        val anUrl = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"
        whenever(htmlService.get(anUrl)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/quotidien.root-without-picture.html"))
        whenever(imageService.getCoverFromURL(any())).then { i -> Cover().apply { url = i.getArgument(0) } }

        /* When */
        val podcast = finder.find(anUrl)

        /* Then */
        assertThat(podcast.title).isEqualTo("Quotidien avec Yann Barthès - TMC | MYTF1")
        assertThat(podcast.description).isEqualTo("Yann Barthès est désormais sur TMC et TF1")
        assertThat(podcast.url).isEqualTo(anUrl)
        assertThat(podcast.type).isEqualTo("TF1Replay")
        assertThat(podcast.cover!!.url).isNull()
    }

    @Test
    fun `should return default podcast if request end up without result`() {
        /* Given */
        val anUrl = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"
        whenever(htmlService.get(anUrl)).thenReturn(None.toVΛVΓ())

        /* When */
        val podcast = finder.find(anUrl)

        /* Then */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST)
    }

    @Test
    fun `should have default cover if nothing comming back from backend server`() {
        /* Given */
        val anUrl = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"
        whenever(htmlService.get(anUrl)).thenReturn(fileAsHtml("/remote/podcast/tf1replay/quotidien.root.html"))
        whenever(imageService.getCoverFromURL(any())).thenReturn(null)

        /* When */
        val podcast = finder.find(anUrl)

        /* Then */
        assertThat(podcast.title).isEqualTo("Quotidien avec Yann Barthès - TMC | MYTF1")
        assertThat(podcast.description).isEqualTo("Yann Barthès est désormais sur TMC et TF1")
        assertThat(podcast.url).isEqualTo(anUrl)
        assertThat(podcast.type).isEqualTo("TF1Replay")
        assertThat(podcast.cover).isSameAs(Cover.DEFAULT_COVER)

    }

    @Test
    fun `should be compatible`() {
        /* Given */
        val url = "https://www.tf1.fr/tmc/quotidien-avec-yann-barthes/videos/replay"
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

    @TestConfiguration
    @Import(TF1ReplayFinder::class)
    class LocalTestConfiguration {
        @Bean fun htmlService() = mock<HtmlService>()
        @Bean fun imageService() = mock<ImageService>()
    }
}
