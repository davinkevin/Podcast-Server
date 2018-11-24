package com.github.davinkevin.podcastserver.manager.worker.dailymotion

import com.github.davinkevin.podcastserver.IOUtils.fileAsJson
import com.github.davinkevin.podcastserver.service.ImageService
import com.nhaarman.mockitokotlin2.whenever
import com.github.davinkevin.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 21/03/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class DailymotionFinderTest {

    @Mock lateinit var imageService: ImageService
    @Mock lateinit var jsonService: JsonService
    @InjectMocks lateinit var dailymotionFinder: DailymotionFinder

    @Test
    fun `should find podcast`() {
        /* Given */
        val podcastUrl = "http://www.dailymotion.com/karimdebbache"
        val cover = Cover().apply { url = "http://s2.dmcdn.net/PB4mc/720x720-AdY.jpg"; width = 200; height = 200 }

        whenever(imageService.getCoverFromURL("http://s2.dmcdn.net/PB4mc/720x720-AdY.jpg")).thenReturn(cover)
        whenever(jsonService.parseUrl("https://api.dailymotion.com/user/karimdebbache?fields=avatar_720_url,description,username"))
                .then { fileAsJson("/remote/podcast/dailymotion/karimdebbache.json") }

        /* When */
        val podcast = dailymotionFinder.find(podcastUrl)

        /* Then */
        assertThat(podcast.description).isEqualTo("CHROMA est une CHROnique de cinéMA sur Dailymotion, dont la première saison se compose de dix épisodes, à raison d’un par mois, d’une durée comprise entre quinze et vingt minutes. Chaque épisode est consacré à un film en particulier.")
        assertThat(podcast.cover).isEqualTo(cover)
        assertThat(podcast.title).isEqualTo("karimdebbache")
        assertThat(podcast.type).isEqualTo("Dailymotion")
    }

    @Test
    fun `should not find podcast`() {
        /* Given */
        val url = "http://iojafea/fake/url"
        /* When */
        val podcast = dailymotionFinder.find(url)
        /* Then */
        assertThat(podcast).isEqualTo(Podcast.DEFAULT_PODCAST)
    }

    @Test
    fun `should be compatible`() {
        assertThat(dailymotionFinder.compatibility("http://www.dailymotion.com/karimdebbache")).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        assertThat(dailymotionFinder.compatibility("http://iojafea/fake/url")).isEqualTo(Integer.MAX_VALUE)
    }
}
