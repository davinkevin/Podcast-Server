package com.github.davinkevin.podcastserver.manager.worker.francetv

import com.github.davinkevin.podcastserver.IOUtils
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Cover
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 08/03/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class FranceTvFinderTest {

    @Mock lateinit var htmlService: HtmlService
    @Mock lateinit var imageService: ImageService
    @InjectMocks lateinit var franceTvFinder: FranceTvFinder

    @Test
    fun `should find podcast`() {
        /* Given */
        val podcastUrl = "https://www.france.tv/france-2/secrets-d-histoire/"
        val coverUrl = "https://www.france.tv/image/carre/265/265/o/c/f/f3b67d5d-phpijifco.png"
        val cover = Cover().apply {
            url = coverUrl
            width = 200
            height = 200
        }

        whenever(htmlService.get(podcastUrl)).thenReturn(IOUtils.fileAsHtml(from("secrets-d-histoire.v2.html")))
        whenever(imageService.getCoverFromURL(coverUrl)).thenReturn(cover)

        /* When */
        val podcast = franceTvFinder.find(podcastUrl)

        /* Then */
        assertThat(podcast.url).isEqualTo(podcastUrl)
        assertThat(podcast.title).isEqualTo("Secrets d'Histoire")
        assertThat(podcast.type).isEqualTo("FranceTv")
        assertThat(podcast.cover).isEqualTo(cover)
        assertThat(podcast.description).isEqualTo("""Secrets d'Histoire est une émission de télévision présentée par Stéphane Bern. Chaque numéro retrace la vie d'un grand personnage de l'histoire et met en lumière des lieux hautement emblématiques du patrimoine.
Magazine Secrets d'Histoire
Accessible à tous, le magazine Secrets d’Histoire vous entraîne au cœur des épisodes mystérieux de l’histoire à travers des reportages, des enquêtes, des quizz… et bien plus encore ! En savoir plus""")
    }

    @Test
    fun `should be compatible`() {
        /* GIVEN */
        val url = "https://www.france.tv/foo/bar/toto"
        /* WHEN  */
        val compatibility = franceTvFinder.compatibility(url)
        /* THEN  */
        assertThat(compatibility).isEqualTo(1)
    }

    @Test
    fun `should not be compatible`() {
        /* GIVEN */
        val url = "https://www.france2.tv/foo/bar/toto"
        /* WHEN  */
        val compatibility = franceTvFinder.compatibility(url)
        /* THEN  */
        assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
    }

    companion object {
        fun from(s: String) = FranceTvUpdaterTest.from(s)
    }
}
