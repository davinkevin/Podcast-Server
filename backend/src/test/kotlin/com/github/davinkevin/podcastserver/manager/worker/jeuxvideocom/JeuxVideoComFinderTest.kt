package com.github.davinkevin.podcastserver.manager.worker.jeuxvideocom

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Podcast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 23/03/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class JeuxVideoComFinderTest {

    @Mock lateinit var htmlService: HtmlService
    @InjectMocks lateinit var finder: JeuxVideoComFinder

    @Test
    fun `should find podcast`() {
        /* Given */
        val url = "www.jeuxvideo.com/foo/bar"
        whenever(htmlService.get(url)).thenReturn(fileAsHtml("/remote/podcast/JeuxVideoCom/chroniques-video.htm"))

        /* When */
        val podcast = finder.find(url)

        /* Then */
        assertThat(podcast.title).isEqualTo("Dernières vidéos de chroniques")
        assertThat(podcast.description).isEqualTo("Découvrez toutes les chroniques de jeux vidéo ainsi que les dernières vidéos de chroniques comme Chronique,Chronique,Chronique,...")
        assertThat(podcast.type).isEqualTo("JeuxVideoCom")
        assertThat(podcast.url).isEqualTo(url)
    }

    @Test
    fun `should not find data for this url`() {
        /* Given */
        whenever(htmlService.get(any())).thenReturn(None.toVΛVΓ())

        /* When */
        val podcast = finder.find("foo/bar")

        /* Then */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST)
    }

    @Test
    fun `should be compatible`() {
        /* Given */
        val url = "www.jeuxvideo.com/foo/bar"

        /* When */
        val compatibility = finder.compatibility(url)

        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* Given */
        val url = "www.youtube.com/foo/bar"

        /* When */
        val compatibility = finder.compatibility(url)

        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }
}
