package com.github.davinkevin.podcastserver.manager.worker.rss

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsXml
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import com.github.davinkevin.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.ZoneOffset

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class RSSUpdaterTest {

    private val rssAppload: Podcast = Podcast().apply { url = "http://mockUrl.com/" }

    @Mock lateinit var signatureService: SignatureService
    @Mock lateinit var jdomService: JdomService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var updater: RSSUpdater

    @Test
    fun `should get items`() {
        /* Given */
        val podcastUrl = "http://mockUrl.com/"
        whenever(jdomService.parse(podcastUrl))
                .thenReturn(fileAsXml("/remote/podcast/rss/rss.appload.xml"))
        whenever(imageService.getCoverFromURL(any())).then { Cover().apply { url = it.getArgument(0) } }

        /* When */
        val items = updater.findItems(rssAppload)

        /* Then */
        assertThat(items).hasSize(217)
        assertThat(items.filter { it.cover == Cover.DEFAULT_COVER }).hasSize(215)
        assertThat(items.filter { it.pubDate == null }).hasSize(1)

        assertThat(items.filter { it.pubDate?.offset == ZoneOffset.ofHours(6) }).hasSize(1)
        assertThat(items.filter { it.pubDate?.offset == ZoneOffset.ofHours(8) }).hasSize(2)
        assertThat(items.filter { it.pubDate?.offset == ZoneOffset.ofHours(9) }).hasSize(1)

        verify(jdomService, times(1)).parse(podcastUrl)
        verify(imageService, times(2)).getCoverFromURL(any())
    }

    @Test
    fun `should return null if not updatable podcast`() {
        /* Given */
        val podcastNotUpdatable = Podcast().apply { url = "http://notUpdatable.com" }
        whenever(jdomService.parse(any())).thenReturn(None.toVΛVΓ())
        /* When */
        val items = updater.findItems(podcastNotUpdatable)
        /* Then */
        assertThat(items).isEmpty()
    }

    @Test
    fun `should return an empty set`() {
        /* Given */
        val podcast = Podcast().apply { url = "http://foo.bar.fake.url/" }
        whenever(jdomService.parse(any())).thenReturn(None.toVΛVΓ())
        /* When */
        val items = updater.findItems(podcast)
        /* Then */
        assertThat(items).isEmpty()
    }

    @Test
    fun `should call signature from url`() {
        /* When */
        updater.signatureOf(rssAppload)
        /* Then */
        verify(signatureService, times(1)).fromUrl("http://mockUrl.com/")
    }

    @Test
    fun `should return its type`() {
        /* Given */
        /* When */
        val type = updater.type()
        /* Then */
        assertThat(type.key).isEqualTo("RSS")
        assertThat(type.name).isEqualTo("RSS")
    }

    @Test
    fun `should be compatible`() {
        /* Given */
        val url = "https://foo.bar.com"
        /* When */
        val compatibility = updater.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE-1)
    }

    @Test
    fun `should not be compatible`() {
        /* Given */
        val url = "grpc://foo.bar.com"
        /* When */
        val compatibility = updater.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }
}
