package com.github.davinkevin.podcastserver.manager.worker.rss

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsXml
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class RSSFinderTest {

    @Mock lateinit var jdomService: JdomService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var rssFinder: RSSFinder

    @Test
    fun `should find information about an rss podcast with his url`() {
        // Given
        val podcastUrl = "http://foo.bar.com/rss.xml"
        whenever(jdomService.parse(podcastUrl)).thenReturn(fileAsXml(from("rss.lesGrandesGueules.xml")))
        whenever(imageService.getCoverFromURL(COVER_URL)).thenReturn(Cover().apply { url = COVER_URL })

        //When
        val podcast = rssFinder.find(podcastUrl)

        //Then
        assertThat(podcast.title).isEqualToIgnoringCase("Les Grandes Gueules du Sport")
        assertThat(podcast.description).isEqualToIgnoringCase("Grand en gueule, fort en sport ! ")
        assertThat(podcast.cover).isNotNull()
        assertThat(podcast.cover.url).isEqualToIgnoringCase(COVER_URL)
    }

    @Test
    fun `should find information with itunes cover`() {
        // Given
        val podcastUrl = "http://foo.bar.com/rss.xml"
        whenever(jdomService.parse(podcastUrl)).thenReturn(fileAsXml(from("rss.lesGrandesGueules.withItunesCover.xml")))
        whenever(imageService.getCoverFromURL(COVER_URL)).thenReturn(Cover().apply { url = COVER_URL })

        //When
        val podcast = rssFinder.find("http://foo.bar.com/rss.xml")

        //Then
        assertThat(podcast.cover).isNotNull()
        assertThat(podcast.cover.url).isEqualTo(COVER_URL)
    }

    @Test
    fun `should find information without any cover`() {
        // Given
        val podcastUrl = "http://foo.bar.com/rss.xml"
        whenever(jdomService.parse(podcastUrl)).thenReturn(fileAsXml(from("rss.lesGrandesGueules.withoutAnyCover.xml")))

        //When
        val podcast = rssFinder.find("http://foo.bar.com/rss.xml")

        //Then
        assertThat(podcast.cover).isEqualTo(Cover.DEFAULT_COVER)
    }

    @Test
    fun `should reject if not found`() {
        /* Given */
        val url = "/remote/podcast/rss/withEmpty.xml"

        whenever(jdomService.parse(url)).thenReturn(None.toVΛVΓ())
        /* When */
        val podcast = rssFinder.find("/remote/podcast/rss/withEmpty.xml")

        /* Then */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST)
    }

    companion object {
        private const val COVER_URL = "http://podcast.rmc.fr/images/podcast_ggdusportjpg_20120831140437.jpg"
        fun from(s: String) = "/remote/podcast/rss/$s"
    }
}
