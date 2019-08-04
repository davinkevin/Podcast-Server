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
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.service.CoverInformation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URI
import java.time.ZoneOffset
import java.util.*

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class RSSUpdaterTest {

    @Mock lateinit var signatureService: SignatureService
    @Mock lateinit var jdomService: JdomService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var updater: RSSUpdater

    private val rssAppload: PodcastToUpdate = PodcastToUpdate(
            url = URI("http://mockUrl.com/"),
            id = UUID.randomUUID(),
            signature = "noSign"
    )

    @Test
    fun `should get items`() {
        /* Given */
        val podcastUrl = "http://mockUrl.com/"
        whenever(jdomService.parse(podcastUrl)).thenReturn(fileAsXml("/remote/podcast/rss/rss.appload.xml"))
        whenever(imageService.fetchCoverInformation(any())).then { CoverInformation (
                url = URI(it.getArgument(0)),
                height = 200,
                width = 200
        ) }

        /* When */
        val items = updater.findItems(rssAppload)

        /* Then */
        assertThat(items).hasSize(217)
        assertThat(items.filter { it.cover == null }).hasSize(215)
        assertThat(items.filter { it.pubDate == null }).hasSize(1)

        assertThat(items.filter { it.pubDate?.offset == ZoneOffset.ofHours(6) }).hasSize(1)
        assertThat(items.filter { it.pubDate?.offset == ZoneOffset.ofHours(8) }).hasSize(2)
        assertThat(items.filter { it.pubDate?.offset == ZoneOffset.ofHours(9) }).hasSize(1)

        verify(jdomService, times(1)).parse(podcastUrl)
        verify(imageService, times(2)).fetchCoverInformation(any())
    }

    @Test
    fun `should return null if not updatable podcast`() {
        /* Given */
        val podcastNotUpdatable = rssAppload.copy( url = URI("http://notUpdatable.com") )
        whenever(jdomService.parse(any())).thenReturn(None.toVΛVΓ())

        /* When */
        val items = updater.findItems(podcastNotUpdatable)

        /* Then */
        assertThat(items).isEmpty()
    }

    @Test
    fun `should call signature from url`() {
        /* When */
        updater.signatureOf(rssAppload.url)

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
