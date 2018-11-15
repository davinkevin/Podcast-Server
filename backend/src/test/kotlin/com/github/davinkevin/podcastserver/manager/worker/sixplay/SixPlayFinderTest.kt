package com.github.davinkevin.podcastserver.manager.worker.sixplay

import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.service.JsonService
import lan.dk.utils.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 26/03/2017
 */
@ExtendWith(MockitoExtension::class)
class SixPlayFinderTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var imageService: ImageService
    @Mock lateinit var jsonService: JsonService
    @InjectMocks lateinit var finder: SixPlayFinder

    @Test
    fun `should find podcast`() {
        /* GIVEN */
        val podcastUrl = "http://www.6play.fr/custom-show"
        whenever(htmlService.get(podcastUrl)).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-main.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(imageService.getCoverFromURL(any())).then { Cover().apply { url = it.getArgument<String>(0) } }

        /* WHEN  */
        val podcast = finder.find(podcastUrl)

        /* THEN  */
        assertThat(podcast.title).isEqualTo("Le Message de Madénian et VDB")
        assertThat(podcast.description).isEqualTo("Mathieu Madénian et Thomas VDB ont des choses à leur dire, à vous dire...")
        assertThat(podcast.type).isEqualTo("SixPlay")
        assertThat(podcast.cover.url).isEqualTo("https://images.6play.fr/v1/images/927766/raw?width=1024&height=576&fit=max&quality=60&format=jpeg&interlace=1&hash=f9a9603fe9b42e1cbb2e11b9e892f1dc0b2c5981")
        verify(imageService, times(1)).getCoverFromURL(any())
    }

    @Test
    fun `should find podcast without description and without cover`() {
        /* GIVEN */
        val podcastUrl = "http://www.6play.fr/custom-show"
        whenever(htmlService.get(podcastUrl)).thenReturn(IOUtils.fileAsHtml("/remote/podcast/6play/mm-vdb-main-without-description.html"))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }

        /* WHEN  */
        val podcast = finder.find(podcastUrl)

        /* THEN  */
        assertThat(podcast.title).isEqualTo("Le Message de Madénian et VDB")
        assertThat(podcast.type).isEqualTo("SixPlay")
        assertThat(podcast.description).isNull()
    }

    @Test
    fun `should throw exception if error during exceution`() {
        /* GIVEN */
        val url = "http://www.6play.fr/custom-show"
        whenever(htmlService.get(any())).thenThrow(RuntimeException("An error occurred"))

        /* WHEN  */
        assertThatThrownBy { finder.find(url) }
                /* THEN  */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("An error occurred")
    }

    @Test
    fun `should be only compatible with 6play url`() {
        assertThat(finder.compatibility(null)).isGreaterThan(1)
        assertThat(finder.compatibility("foo")).isGreaterThan(1)
        assertThat(finder.compatibility("http://www.6play.fr/test")).isEqualTo(1)
    }
}
