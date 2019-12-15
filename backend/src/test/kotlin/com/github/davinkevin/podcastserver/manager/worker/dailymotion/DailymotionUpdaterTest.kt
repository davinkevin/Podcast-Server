package com.github.davinkevin.podcastserver.manager.worker.dailymotion

import com.github.davinkevin.podcastserver.fileAsJson
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URI
import java.util.*

/**
 * Created by kevin on 22/02/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class DailymotionUpdaterTest {

    @Mock lateinit var signatureService: SignatureService
    @Mock lateinit var jsonService: JsonService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var updater: DailymotionUpdater

    val podcast = PodcastToUpdate(
        id = UUID.randomUUID(),
        url = URI("http://www.dailymotion.com/karimdebbache"),
        signature = ""
    )

    @Test
    fun `should sign from url`() {
        /* Given */
        whenever(signatureService.fromUrl(API_LIST_OF_ITEMS.format("karimdebbache")))
                .thenReturn("aSignature")

        /* When */
        val s = updater.blockingSignatureOf(podcast.url)

        /* Then */
        assertThat(s).isEqualTo("aSignature")
    }

    @Test
    fun `should get items`() {
        /* Given */
        val karimdebbache = API_LIST_OF_ITEMS.format("karimdebbache")
        whenever(jsonService.parseUrl(karimdebbache))
                .then { fileAsJson("/remote/podcast/dailymotion/user.karimdebbache.json") }

        /* When */
        val items = updater.blockingFindItems(podcast)

        /* Then */
        assertThat(items).hasSize(10)
    }

    @Test
    fun `should get empty list if error during fetching`() {
        /* Given */
        val karimdebbache = API_LIST_OF_ITEMS.format("karimdebbache")
        whenever(jsonService.parseUrl(karimdebbache)).thenReturn(arrow.core.None)

        /* When */
        val items = updater.blockingFindItems(podcast)

        /* Then */
        assertThat(items).isEmpty()
    }

    @Test
    fun `should get empty list if error of parsing url`() {
        /* Given */
        val otherPodcast = podcast.copy(url = URI("http://foo.bar/goo"))

        /* When */
        assertThatThrownBy { updater.blockingSignatureOf(otherPodcast.url) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Username not Found")
    }

    @Test
    fun `should have type`() {
        assertThat(updater.type().name).isEqualTo("Dailymotion")
        assertThat(updater.type().key).isEqualTo("Dailymotion")
    }

    @Test
    fun `should be compatible`() {
        assertThat(updater.compatibility("https://www.dailymotion.com/foo/bar"))
                .isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        assertThat(updater.compatibility("https://www.not_dailymotion.com/foo/bar"))
                .isEqualTo(Integer.MAX_VALUE)
    }

    @Test
    fun `should handle null case for compatibility`() {
        assertThat(updater.compatibility(null)).isEqualTo(Integer.MAX_VALUE)
    }

    companion object {
        const val API_LIST_OF_ITEMS = "https://api.dailymotion.com/user/%s/videos?fields=created_time,description,id,thumbnail_720_url,title"
    }
}
