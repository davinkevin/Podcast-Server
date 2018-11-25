package com.github.davinkevin.podcastserver.manager.worker.youtube

import arrow.core.None
import com.github.davinkevin.podcastserver.IOUtils.fileAsHtml
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.whenever
import com.github.davinkevin.podcastserver.entity.Podcast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class YoutubeFinderTest {

    @Mock lateinit var htmlService: HtmlService
    @InjectMocks lateinit var finder: YoutubeFinder

    @Test
    fun `should find information about a youtube podcast with his url`() {
        //Given
        whenever(htmlService.get("https://www.youtube.com/user/cauetofficiel"))
                .thenReturn(fileAsHtml("/remote/podcast/youtube/youtube.cauetofficiel.html"))

        //When
        val podcast = finder.find("https://www.youtube.com/user/cauetofficiel")
        val cover = podcast.cover

        //Then
        assertThat(podcast.title).isEqualTo("Cauet")
        assertThat(podcast.description).isEqualTo("La chaîne officielle de Cauet, c'est toujours plus de kiff et de partage ! Des vidéos exclusives de C'Cauet sur NRJ tous les soirs de 19h à 22h. Des défis in...")

        assertThat(cover).isNotNull
        assertThat(cover!!.url).isEqualTo("https://yt3.ggpht.com/-83tzNbjW090/AAAAAAAAAAI/AAAAAAAAAAA/Vj6_1jPZOVc/s100-c-k-no/photo.jpg")
    }

    @Test
    fun `should not find podcast for this url`() {
        /* Given */
        whenever(htmlService.get("https://www.youtube.com/user/cauetofficiel")).thenReturn(None.toVΛVΓ())

        /* When */
        val podcast = finder.find("https://www.youtube.com/user/cauetofficiel")

        /* Then -> See @Test Exception*/
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST)
    }

    @Test
    fun `should set default value for information not found`() {
        //Given
        whenever(htmlService.get("https://www.youtube.com/user/cauetofficiel"))
                .thenReturn(fileAsHtml("/remote/podcast/youtube/youtube.cauetofficiel.withoutDescAndCoverAndTitle.html"))

        //When
        val podcast = finder.find("https://www.youtube.com/user/cauetofficiel")
        val cover = podcast.cover

        //Then
        assertThat(podcast.title).isEmpty()
        assertThat(podcast.description).isEmpty()
        assertThat(cover).isNotNull()
        assertThat(cover!!.url).isNull()
    }

    @DisplayName("should be compatible with")
    @ParameterizedTest(name = "with {0}")
    @ValueSource(strings = [
        "http://www.youtube.com/channel/a-channel", "http://youtube.com/user/foo-User",
        "https://gdata.youtube.com/feeds/api/playlists/UE1987158913731", "https://another.youtube.com/bar-foo"
    ])
    fun `should be compatible with`(/* Given */ url: String) {
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* Given */
        val url = "http://foo.bar.com/"
        /* When */
        val compatibility = finder.compatibility(url)
        /* Then */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }
}
