package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.business.stats.NumberOfItemByDateWrapper
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import java.time.LocalDate
import java.util.*
import com.github.davinkevin.podcastserver.podcast.PodcastRepositoryV2 as PodcastRepository

/**
 * Created by kevin on 2019-02-16
 */
@ExtendWith(SpringExtension::class)
@Import(PodcastService::class)
class PodcastServiceTest {

    @Autowired lateinit var service: PodcastService
    @MockBean lateinit var repository: PodcastRepository

    val podcast = Podcast(
            id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
            title = "Podcast title",

            cover = CoverForPodcast(
                    id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                    url = "https://external.domain.tld/cover.png",
                    height = 200, width = 200
            )
    )

    @Test
    fun `should find by id`() {
        /* Given */
        whenever(repository.findById(podcast.id)).thenReturn(podcast.toMono())
        /* When */
        StepVerifier.create(service.findById(podcast.id))
                /* Then */
                .expectSubscription()
                .expectNext(podcast)
                .verifyComplete()
    }

    @Nested
    @DisplayName("should find stats")
    inner class ShouldFindStats {

        val r = listOf(
                NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                NumberOfItemByDateWrapper(LocalDate.parse("2019-01-12"), 2),
                NumberOfItemByDateWrapper(LocalDate.parse("2019-01-28"), 6)
        )

        @Test
        fun `should find stats by pubDate`() {
            /* Given */
            whenever(repository.findStatByPodcastIdAndPubDate(podcast.id, 3)).thenReturn(r.toFlux())
            /* When */
            StepVerifier.create(service.findStatByPodcastIdAndPubDate(podcast.id, 3))
                    /* Then */
                    .expectSubscription()
                    .expectNextSequence(r)
                    .verifyComplete()
        }

        @Test
        fun `should find stats by downloadDate`() {
            /* Given */
            whenever(repository.findStatByPodcastIdAndDownloadDate(podcast.id, 3)).thenReturn(r.toFlux())
            /* When */
            StepVerifier.create(service.findStatByPodcastIdAndDownloadDate(podcast.id, 3))
                    /* Then */
                    .expectSubscription()
                    .expectNextSequence(r)
                    .verifyComplete()
        }

        @Test
        fun `should find stats by creationDate`() {
            /* Given */
            whenever(repository.findStatByPodcastIdAndCreationDate(podcast.id, 3)).thenReturn(r.toFlux())
            /* When */
            StepVerifier.create(service.findStatByPodcastIdAndCreationDate(podcast.id, 3))
                    /* Then */
                    .expectSubscription()
                    .expectNextSequence(r)
                    .verifyComplete()
        }

    }


}
