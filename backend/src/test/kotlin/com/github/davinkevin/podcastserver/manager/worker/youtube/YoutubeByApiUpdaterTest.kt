package com.github.davinkevin.podcastserver.manager.worker.youtube

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.IOUtils.fileAsJson
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.service.properties.Api
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.*
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 16/09/2018
 */
@ExtendWith(MockitoExtension::class)
class YoutubeByApiUpdaterTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var jsonService: JsonService
    @Mock lateinit var api: Api
    @Mock lateinit var signatureService: SignatureService
    @InjectMocks lateinit var updater: YoutubeByApiUpdater

    @Test
    fun `should get items with API from channel`() {
        /* Given */
        val page1 = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=FOO"
        val page2 = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=FOO&pageToken=CDIQAA"
        val podcast = Podcast().apply { url = "https://www.youtube.com/user/joueurdugrenier"; items = setOf() }

        whenever(api.youtube).thenReturn("FOO")
        doAnswer{ fileAsJson("/remote/podcast/youtube/joueurdugrenier.json")   }.whenever(jsonService).parseUrl(page1)
        doAnswer{ fileAsJson("/remote/podcast/youtube/joueurdugrenier.2.json") }.whenever(jsonService).parseUrl(page2)
        whenever(htmlService.get(podcast.url)).thenReturn(fileAsHtml("/remote/podcast/youtube/joueurdugrenier.html"))

        /* When */
        val items = updater.findItems(podcast)

        /* Then */
        assertThat(items).hasSize(87)
        verify(jsonService, times(2)).parseUrl(any())
    }

    @Test
    fun `should handle error during fetch items`() {
        /* Given */
        whenever(api.youtube).thenReturn("FOO")
        val podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .items(setOf())
                .build()

        whenever(htmlService.get(any())).thenReturn(fileAsHtml("/remote/podcast/youtube/androiddevelopers.html"))
        whenever(jsonService.parseUrl(any())).thenReturn(None.toVΛVΓ())

        /* When */
        val items = updater.findItems(podcast)

        /* Then */
        assertThat(items).hasSize(0)
        verify(jsonService, times(1)).parseUrl(any())
    }

    @Test
    fun `should sign with api key`() {
        /* Given */
        val page1 = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=FOO"
        val podcast = Podcast().apply { url = "https://www.youtube.com/user/joueurdugrenier"; items = setOf() }

        whenever(api.youtube).thenReturn("FOO")
        whenever(htmlService.get(podcast.url)).thenReturn(fileAsHtml("/remote/podcast/youtube/joueurdugrenier.html"))
        doAnswer{ fileAsJson("/remote/podcast/youtube/joueurdugrenier.json")   }.whenever(jsonService).parseUrl(page1)
        whenever(signatureService.fromText(any())).thenCallRealMethod()

        /* When */
        val signature = updater.signatureOf(podcast)

        /* Then */
        assertThat(signature).isEqualTo("64cc064a14dba90a0df24218db758479")
    }

    @Test
    fun `should return empty if no result`() {
        /* Given */
        val podcast = Podcast().apply { url = "https://www.youtube.com/user/joueurdugrenier"; items = setOf() }

        whenever(api.youtube).thenReturn("FOO")
        whenever(htmlService.get(any())).thenReturn(fileAsHtml("/remote/podcast/youtube/androiddevelopers.html"))
        whenever(jsonService.parseUrl(any())).thenReturn(None.toVΛVΓ())

        /* When */
        val signature = updater.signatureOf(podcast)

        /* Then */
        assertThat(signature).isEmpty()
        verify(signatureService, never()).fromText(any())
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
        Assertions.assertThat(compatibility).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* Given */
        val url = "http://foo.bar.com/"
        /* When */
        val compatibility = updater.compatibility(url)
        /* Then */
        Assertions.assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }

    @Test
    fun `should return youtube type`() {
        val type = updater.type()
        assertThat(type.name).isEqualTo("Youtube")
        assertThat(type.key).isEqualTo("Youtube")
    }
}