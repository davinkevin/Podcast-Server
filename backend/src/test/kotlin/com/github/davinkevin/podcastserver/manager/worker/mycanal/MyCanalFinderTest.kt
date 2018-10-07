package com.github.davinkevin.podcastserver.manager.worker.mycanal

import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.IOUtils.stringAsJson
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 16/03/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class MyCanalFinderTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var imageService: ImageService
    @Mock lateinit var jsonService: JsonService
    @InjectMocks lateinit var finder: MyCanalFinder

    @Test
    fun should_find_podcast() {
        /* Given */
        val coverUrl = "https://thumb.canalplus.pro/http/unsafe/1920x665/top/secure-media.mycanal.fr/image/56/9/mycanal_cover_logotypee_1920x665.25569.jpg"
        val cover = Cover().apply {
            url = coverUrl
            width = 200
            height = 200
        }
        whenever(imageService.getCoverFromURL(coverUrl)).thenReturn(cover)
        whenever(htmlService.get("https://www.mycanal.fr/emissions/pid1319-le-tube.html"))
                .thenReturn(fileAsHtml("/remote/podcast/mycanal/le-tube.html"))
        whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }

        /* When */
        val podcast = finder.find("https://www.mycanal.fr/emissions/pid1319-le-tube.html")

        /* Then */
        assertThat(podcast.url).isEqualTo("https://www.mycanal.fr/theme/emissions/pid1319-le-tube.html")
        assertThat(podcast.title).isEqualTo("Le Tube")
        assertThat(podcast.type).isEqualTo("MyCanal")
        assertThat(podcast.cover).isEqualTo(cover)
    }

    @Test
    fun should_find_podcast_without_landing_page() {
        /* Given */
        whenever(htmlService.get("https://www.mycanal.fr/theme/emissions/pid4936-j-1.html"))
                .thenReturn(fileAsHtml("/remote/podcast/mycanal/j_plus_1.html"))
        whenever(jsonService.parse(any())).then { stringAsJson(it.getArgument(0)) }

        /* When */
        val podcast = finder.find("https://www.mycanal.fr/theme/emissions/pid4936-j-1.html")

        /* Then */
        assertThat(podcast.url).isEqualTo("https://www.mycanal.fr/theme/emissions/pid4936-j-1.html")
        assertThat(podcast.title).isEqualTo("J+1")
        assertThat(podcast.type).isEqualTo("MyCanal")
        assertThat(podcast.cover).isEqualTo(Cover.DEFAULT_COVER)
    }

    @Test
    fun should_return_nothing_if_structure_has_change() {
        /* GIVEN */
        whenever(htmlService.get("https://www.mycanal.fr/emissions/pid1319-le-tube.html"))
                .thenReturn(fileAsHtml("/remote/podcast/mycanal/le-tube_without-data.html"))
        /* When */
        val podcast = finder.find("https://www.mycanal.fr/emissions/pid1319-le-tube.html")
        /* THEN  */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST)
    }

    @Test
    fun should_be_compatible() {
        assertThat(finder.compatibility("https://www.mycanal.fr/emissions/pid1319-le-tube.html")).isEqualTo(1)
    }

    @Test
    fun should_not_be_compatible() {
        assertThat(finder.compatibility("http://www.foo.fr/bar/to.html")).isGreaterThan(1)
    }

}
