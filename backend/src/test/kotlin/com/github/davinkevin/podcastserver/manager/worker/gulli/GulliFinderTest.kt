package com.github.davinkevin.podcastserver.manager.worker.gulli

import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 13/10/2016
 */
@ExtendWith(MockitoExtension::class)
class GulliFinderTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var finder: GulliFinder

    @Test
    fun `should_find a podcast for valid url`() {
        /* Given */
        val podcastUrl = "http://replay.gulli.fr/dessins-animes/Pokemon3"
        val coverUrl = "http://resize1-gulli.ladmedia.fr/r/340,255,smartcrop,center-top/img/var/storage/imports/replay/images_programme/pokemon_s19.jpg"

        whenever(htmlService.get(podcastUrl)).thenReturn(fileAsHtml("/remote/podcast/gulli/pokemon.html"))
        whenever(imageService.getCoverFromURL(coverUrl))
                .then { Cover().apply { url = it.getArgument(0); height = 250; width = 250 } }

        /* When */
        val podcast = finder.find(podcastUrl)

        /* Then */
        assertThat(podcast.url).isEqualTo(podcastUrl)
        assertThat(podcast.title).isEqualTo("Pokémon")
        assertThat(podcast.type).isEqualTo("Gulli")

        val cover = podcast.cover!!
        assertThat(cover.url).isEqualTo(coverUrl)
        assertThat(cover.height).isEqualTo(250)
        assertThat(cover.width).isEqualTo(250)
    }

    @Test
    fun `should be compatible`() {
        assertThat(finder.compatibility("http://replay.gulli.fr/dessins-animes/Pokemon3"))
                .isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        assertThat(finder.compatibility("http://foo.bar.fr/dessins-animes/Pokemon3"))
                .isEqualTo(Integer.MAX_VALUE)
    }
}
