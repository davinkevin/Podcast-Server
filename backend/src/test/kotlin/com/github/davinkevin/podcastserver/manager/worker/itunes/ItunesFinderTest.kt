package com.github.davinkevin.podcastserver.manager.worker.itunes

import com.github.davinkevin.podcastserver.IOUtils
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.rss.RSSFinder
import lan.dk.podcastserver.service.JsonService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 12/05/2018
 */
@ExtendWith(MockitoExtension::class)
class ItunesFinderTest {

    @Mock lateinit var rssFinder: RSSFinder
    @Mock lateinit var jsonService: JsonService
    @InjectMocks lateinit var finder: ItunesFinder

    @Test
    fun `should be compatible with itunes url`() {
        /* GIVEN */
        val url = "https://itunes.apple.com/fr/podcast/cauet-sl%C3%A2che/id1278255446?l=en&mt=2"
        /* WHEN  */
        val compatibilityLevel = finder.compatibility(url)
        /* THEN  */
        assertThat(compatibilityLevel).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* GIVEN */
        val url = "https://foo.bar.com/fr/podcast/foo/idbar"
        /* WHEN  */
        val compatibilityLevel = finder.compatibility(url)
        /* THEN  */
        assertThat(compatibilityLevel).isGreaterThan(1)
    }

    @Test
    fun `should find url`() {
        /* GIVEN */
        val url = "https://itunes.apple.com/fr/podcast/cauet-sl%C3%A2che/id1278255446?l=en&mt=2"
        val p = Podcast()
        whenever(jsonService.parseUrl("https://itunes.apple.com/lookup?id=1278255446"))
                .thenReturn(IOUtils.fileAsJson(from("lookup.json")))
        whenever(rssFinder.find("https://www.virginradio.fr/cauet-s-lache/podcasts.podcast")).thenReturn(p)
        /* WHEN  */
        val podcast = finder.find(url)
        /* THEN  */
        assertThat(podcast).isSameAs(p)
    }

    @Test
    fun `should return default podcast if nothing found`() {
        /* GIVEN */
        val url = "https://foo.bar.com/ofejaoieaf/aekofjaeoi"
        /* WHEN  */
        val podcast = finder.find(url)
        /* THEN  */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST)
    }

    private fun from(name: String) = "/remote/podcast/itunes/$name"
}