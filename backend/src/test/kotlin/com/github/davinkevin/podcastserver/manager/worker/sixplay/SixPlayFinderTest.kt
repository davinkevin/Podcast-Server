package com.github.davinkevin.podcastserver.manager.worker.sixplay

import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.service.JsonService
import com.github.davinkevin.podcastserver.IOUtils
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
        whenever(htmlService.get(podcastUrl)).thenReturn(IOUtils.fileAsHtml(of("sport-6-p_1380.html")))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(imageService.getCoverFromURL(any())).then { Cover().apply { url = it.getArgument<String>(0) } }

        /* WHEN  */
        val podcast = finder.find(podcastUrl)

        /* THEN  */
        assertThat(podcast.title).isEqualTo("Sport 6")
        assertThat(podcast.type).isEqualTo("SixPlay")
        assertThat(podcast.cover.url).isEqualTo("https://images.6play.fr/v2/images/598896/raw?width=1024&height=576&fit=max&quality=60&format=jpeg&interlace=1&hash=33abccc9be94554a7081bb8cc10a1fe94b8fefa0")
        assertThat(podcast.description).isEqualTo("Retrouvez chaque semaine toute l'actualité et les résultats du sport dans Sport 6. En 6 minutes, priorités aux images : les temps forts de l'actualité et les résultats sportifs sont décryptés pour tout connaître des faits marquants de la semaine.")
        verify(imageService, times(1)).getCoverFromURL(any())
    }

    @Test
    fun `should find podcast without description and without cover`() {
        /* GIVEN */
        val podcastUrl = "http://www.6play.fr/custom-show"
        whenever(htmlService.get(podcastUrl)).thenReturn(IOUtils.fileAsHtml(of("sport-6-p_1380-without-description.html")))
        whenever(jsonService.parse(any())).then { IOUtils.stringAsJson(it.getArgument(0)) }
        whenever(imageService.getCoverFromURL(any())).then { Cover().apply { url = it.getArgument<String>(0) } }

        /* WHEN  */
        val podcast = finder.find(podcastUrl)

        /* THEN  */
        assertThat(podcast.title).isEqualTo("Sport 6")
        assertThat(podcast.type).isEqualTo("SixPlay")
        assertThat(podcast.cover.url).isEqualTo("https://images.6play.fr/v2/images/598896/raw?width=1024&height=576&fit=max&quality=60&format=jpeg&interlace=1&hash=33abccc9be94554a7081bb8cc10a1fe94b8fefa0")
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

    companion object {

        private fun of(filename: String): String {
            return "/remote/podcast/6play/$filename"
        }

    }
}
