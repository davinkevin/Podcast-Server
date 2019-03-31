package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.tag.Tag
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
            url = "https://foo.bar.com/app/file.rss",
            hasToBeDeleted = true,
            lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
            type = "RSS",
            tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

            cover = CoverForPodcast(
                    id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                    url = URI("https://external.domain.tld/cover.png"),
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

        @Nested
        @DisplayName("by podcast id")
        inner class ByPodcastId {

            @Test
            fun `by pubDate`() {
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
            fun `by downloadDate`() {
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
            fun `by creationDate`() {
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

        @Nested
        @DisplayName("globally")
        inner class Globally {

            val s = listOf(
                    NumberOfItemByDateWrapper(LocalDate.parse("2019-01-02"), 3),
                    NumberOfItemByDateWrapper(LocalDate.parse("2019-02-12"), 2),
                    NumberOfItemByDateWrapper(LocalDate.parse("2019-03-28"), 6)
            )

            val youtube = StatsPodcastType("YOUTUBE", s.toSet())
            val rss = StatsPodcastType("RSS", r.toSet())

            @Test
            fun `by pubDate`() {
                /* Given */
                whenever(repository.findStatByTypeAndPubDate(3)).thenReturn(Flux.just(youtube, rss))
                /* When */
                StepVerifier.create(service.findStatByTypeAndPubDate(3))
                        /* Then */
                        .expectSubscription()
                        .expectNext(youtube)
                        .expectNext(rss)
                        .verifyComplete()
            }

            @Test
            fun `by downloadDate`() {
                /* Given */
                whenever(repository.findStatByTypeAndDownloadDate(3)).thenReturn(Flux.just(youtube, rss))
                /* When */
                StepVerifier.create(service.findStatByTypeAndDownloadDate(3))
                        /* Then */
                        .expectSubscription()
                        .expectNext(youtube)
                        .expectNext(rss)
                        .verifyComplete()
            }

            @Test
            fun `by creationDate`() {
                /* Given */
                whenever(repository.findStatByTypeAndCreationDate(3)).thenReturn(Flux.just(youtube, rss))
                /* When */
                StepVerifier.create(service.findStatByTypeAndCreationDate(3))
                        /* Then */
                        .expectSubscription()
                        .expectNext(youtube)
                        .expectNext(rss)
                        .verifyComplete()
            }

        }

    }

}
