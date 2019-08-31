package com.github.davinkevin.podcastserver.manager.worker.youtube

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.IOUtils.fileAsXml
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.update.updaters.youtube.YoutubeByXmlUpdater
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URI
import java.util.*

/**
 * Created by kevin on 16/09/2018
 */
@ExtendWith(MockitoExtension::class)
class YoutubeByXmlUpdaterTest {

    @Mock private lateinit var jdomService: JdomService
    @Mock private lateinit var htmlService: HtmlService
    @InjectMocks private lateinit var updater: YoutubeByXmlUpdater

    @Test
    fun `should be of type youtube`() {
        /* Given */
        val type = updater.type()

        /* Then */
        assertThat(type).isNotNull()
        assertThat(type.key).isEqualTo("Youtube")
        assertThat(type.name).isEqualTo("Youtube")
    }

    @Test
    fun `should get items for channel`() {
        /* Given */
        val podcast = PodcastToUpdate ( url = URI("https://www.youtube.com/user/androiddevelopers"), id = UUID.randomUUID(), signature = "noSign" )
        whenever(htmlService.get(any())).thenReturn(fileAsHtml("/remote/podcast/youtube/androiddevelopers.html"))
        whenever(jdomService.parse(any())).then { fileAsXml("/remote/podcast/youtube/youtube.androiddevelopers.xml") }

        /* When */
        val items = updater.blockingFindItems(podcast)

        /* Then */
        assertThat(items).hasSize(15)
        verify(jdomService, only()).parse("https://www.youtube.com/feeds/videos.xml?channel_id=UCVHFbqXqoYvEWM1Ddxl0QDg")
        verify(htmlService, only()).get("https://www.youtube.com/user/androiddevelopers")
    }


    @Test
    fun `should get items for playlist`() {
        /* Given */
        val podcast = PodcastToUpdate ( url = URI("https://www.youtube.com/playlist?list=PLAD454F0807B6CB80"), id = UUID.randomUUID(), signature = "noSign" )

        whenever(jdomService.parse(any())).thenReturn(fileAsXml("/remote/podcast/youtube/joueurdugrenier.playlist.xml"))

        /* When */
        val items = updater.blockingFindItems(podcast)

        /* Then */
        assertThat(items).hasSize(15)
        verify(jdomService, only()).parse("https://www.youtube.com/feeds/videos.xml?playlist_id=PLAD454F0807B6CB80")
    }

    @Test
    fun `should generate signature`() {
        /* Given */
        val podcast = PodcastToUpdate ( url = URI("https://www.youtube.com/user/androiddevelopers"), id = UUID.randomUUID(), signature = "noSign" )

        whenever(htmlService.get(any())).thenReturn( fileAsHtml("/remote/podcast/youtube/androiddevelopers.html"))
        whenever(jdomService.parse(any())).thenReturn( fileAsXml("/remote/podcast/youtube/youtube.androiddevelopers.xml"))

        /* When */
        val signature = updater.blockingSignatureOf(podcast.url)

        /* Then */
        assertThat(signature).isEqualTo("e2ceb3426f7631396a49f16942270c02")
        verify(jdomService, only()).parse("https://www.youtube.com/feeds/videos.xml?channel_id=UCVHFbqXqoYvEWM1Ddxl0QDg")
        verify(htmlService, only()).get("https://www.youtube.com/user/androiddevelopers")
    }

    @Test
    fun `should handle error during signature`() {
        val podcast = Podcast().apply {
            url = "https://www.youtube.com/user/androiddevelopers"
        }

        whenever(htmlService.get(any())).thenReturn( fileAsHtml("/remote/podcast/youtube/androiddevelopers.html"))
        whenever(jdomService.parse(any())).thenReturn(None.toVΛVΓ())

        /* When */
        val signature = updater.blockingSignatureOf(URI(podcast.url!!))

        /* Then */
        assertThat(signature).isEmpty()
    }

    @Test
    fun `should return empty if parsing error`() {
        /* Given */
        val podcast = PodcastToUpdate ( url = URI("https://www.youtube.com/feeds/videos.xml?playlist_id=PLYMLK0zkSFQTblsW2biu2m4suKvoomN5D"), id = UUID.randomUUID(), signature = "noSign" )

        whenever(htmlService.get(any())).thenReturn(None.toVΛVΓ())
        whenever(jdomService.parse(any())).thenReturn(None.toVΛVΓ())

        /* When */
        val items = updater.blockingFindItems(podcast)

        /* Then */
        assertThat(items).hasSize(0)
    }

    @Test
    fun `should return empty set because html page not found`() {
        /* Given */
        val podcast = PodcastToUpdate ( url = URI("https://www.youtube.com/user/androiddevelopers"), id = UUID.randomUUID(), signature = "noSign" )

        whenever(htmlService.get(any())).thenReturn(None.toVΛVΓ())
        whenever(jdomService.parse("https://www.youtube.com/feeds/videos.xml?channel_id=")).thenReturn(None.toVΛVΓ())

        /* When */
        val items = updater.blockingFindItems(podcast)

        /* Then */
        assertThat(items).hasSize(0)
    }

    @Test
    fun `should return empty set because of data tag not find`() {
        /* Given */
        val podcast = PodcastToUpdate ( url = URI("https://www.youtube.com/user/androiddevelopers"), id = UUID.randomUUID(), signature = "noSign" )

        whenever(htmlService.get(any())).thenReturn(None.toVΛVΓ())
        whenever(jdomService.parse("https://www.youtube.com/feeds/videos.xml?channel_id=")).thenReturn(None.toVΛVΓ())

        /* When */
        val items = updater.blockingFindItems(podcast)

        /* Then */
        assertThat(items).hasSize(0)
        verify(jdomService, only()).parse("https://www.youtube.com/feeds/videos.xml?channel_id=")
        verify(htmlService, only()).get("https://www.youtube.com/user/androiddevelopers")
    }

    @Test
    fun `should not be compatible`() {
        /* Given */
        val url = "http://foo.bar/com"
        /* When */
        val compatibility = updater.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }

    @DisplayName("should be compatible with")
    @ParameterizedTest(name = "with {0}")
    @ValueSource(strings = [
        "http://www.youtube.com/channel/a-channel", "http://youtube.com/user/foo-User",
        "https://gdata.youtube.com/feeds/api/playlists/UE1987158913731", "https://another.youtube.com/bar-foo"
    ])
    fun `should be compatible with`(/* Given */ url: String) {
        /* When */
        val compatibility = updater.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

}
