package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.tag.Tag
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.*
import reactor.test.StepVerifier
import java.net.URI
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import com.github.davinkevin.podcastserver.cover.CoverRepositoryV2 as CoverRepository
import com.github.davinkevin.podcastserver.podcast.PodcastRepositoryV2 as PodcastRepository
import com.github.davinkevin.podcastserver.tag.TagRepositoryV2 as TagRepository

/**
 * Created by kevin on 2019-02-16
 */
@ExtendWith(SpringExtension::class)
@Import(PodcastService::class)
class PodcastServiceTest {

    @Autowired lateinit var service: PodcastService
    @MockBean lateinit var coverRepository: CoverRepository
    @MockBean lateinit var tagRepository: TagRepository
    @MockBean lateinit var repository: PodcastRepository
    @MockBean lateinit var fileService: FileService

    val podcast = Podcast(
            id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
            title = "Podcast title",
            description = "desc",
            signature = null,
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
    @DisplayName("should find all")
    inner class ShouldFindAll {

        private val podcast1 = Podcast(
                id = UUID.fromString("ad16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast first",
                description = "desc",
                signature = null,
                url = "https://foo.bar.com/app/1.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = CoverForPodcast(
                        id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                        url = URI("https://external.domain.tld/1.png"),
                        height = 200, width = 200
                )
        )
        private val podcast2 = Podcast(
                id = UUID.fromString("bd16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast second",
                description = "desc",
                signature = null,
                url = "https://foo.bar.com/app/2.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = CoverForPodcast(
                        id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                        url = URI("https://external.domain.tld/2.png"),
                        height = 200, width = 200
                )
        )
        private val podcast3 = Podcast(
                id = UUID.fromString("cd16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast third",
                description = "desc",
                signature = null,
                url = "https://foo.bar.com/app/3.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = CoverForPodcast(
                        id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                        url = URI("https://external.domain.tld/3.png"),
                        height = 200, width = 200
                )
        )

        @Test
        fun `with 3 podcasts`() {
            /* Given */
            val podcasts = listOf(podcast1, podcast2, podcast3)
            whenever(repository.findAll()).thenReturn(podcasts.toFlux())
            /* When */
            StepVerifier.create(service.findAll())
                    /* Then */
                    .expectSubscription()
                    .expectNext(podcast1, podcast2, podcast3)
                    .verifyComplete()
        }

        @Test
        fun `with 0 podcast`() {
            /* Given */
            whenever(repository.findAll()).thenReturn(Flux.empty())
            /* When */
            StepVerifier.create(service.findAll())
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

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

    @Suppress("UnassignedFluxMonoInstance")
    @Nested
    @DisplayName("should save podcast")
    inner class ShouldSavePodcast {

        val podcastForCreation = PodcastForCreation(
                title = "Podcast title",
                url = URI("https://foo.bar.com/app/file.rss"),
                hasToBeDeleted = true,
                type = "RSS",
                tags = setOf(
                        TagForCreation(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma"),
                        TagForCreation(UUID.fromString("f9d92928-1c4c-47a5-965d-efbb2d422f0c"), "Sport"),
                        TagForCreation(id = null, name = "new")
                ),
                cover = CoverForCreation(url = URI("https://external.domain.tld/cover.png"), height = 200, width = 200)

        )

        @Nested
        @DisplayName("which doesn't exist before")
        inner class WhichDoesntExistBefore {

            @BeforeEach
            fun beforeEach() {
                whenever(fileService.downloadPodcastCover(podcast)).thenReturn(Mono.empty())
            }

            @AfterEach
            fun afterEach() {
                verify(fileService).downloadPodcastCover(podcast)
                Mockito.reset(fileService)
            }

            @Test
            fun `with no tags and just a cover`() {
                /* Given */
                val tags = emptySet<TagForCreation>()
                val p = podcastForCreation.copy(tags = tags)
                val savedCover = p.cover.toPodcastCover()
                whenever(coverRepository.save(p.cover)).thenReturn(savedCover.toMono())
                whenever(repository.save(eq(p.title), eq(p.url!!.toASCIIString()), eq(p.hasToBeDeleted), eq(p.type), argThat { isEmpty() }, eq(savedCover)))
                        .thenReturn(podcast.toMono())

                /* When */
                StepVerifier.create(service.save(p))
                        /* Then */
                        .expectSubscription()
                        .assertNext { assertThat(it).isSameAs(podcast) }
                        .verifyComplete()
            }


            @Test
            fun `with already existing tags and a cover`() {
                /* Given */
                val tags = listOf(
                        TagForCreation(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma"),
                        TagForCreation(UUID.fromString("f9d92928-1c4c-47a5-965d-efbb2d422f0c"), "Sport")
                )
                val p = podcastForCreation.copy(tags = tags)
                val savedCover = p.cover.toPodcastCover()

                whenever(coverRepository.save(p.cover)).thenReturn(savedCover.toMono())
                whenever(repository.save(
                        eq(p.title),
                        eq(p.url!!.toASCIIString()),
                        eq(p.hasToBeDeleted),
                        eq(p.type),
                        argThat { map { it.id }.containsAll(tags.map { it.id }) && size == 2 },
                        eq(savedCover)
                ))
                        .thenReturn(podcast.toMono())

                /* When */
                StepVerifier.create(service.save(p))
                        /* Then */
                        .expectSubscription()
                        .assertNext { assertThat(it).isSameAs(podcast) }
                        .verifyComplete()
            }

            @Test
            fun `with new and old tags and a cover`() {
                /* Given */
                val newTagId = UUID.randomUUID()
                val oldTagId = UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c")
                val tags = listOf(
                        TagForCreation(oldTagId, "Cinéma"),
                        TagForCreation(null, "Sport")
                )
                val p = podcastForCreation.copy(tags = tags)
                val savedCover = p.cover.toPodcastCover()

                whenever(coverRepository.save(p.cover)).thenReturn(savedCover.toMono())
                whenever(tagRepository.save("Sport")).thenReturn(Tag(newTagId, "Sport").toMono())
                whenever(repository.save(
                        eq(p.title),
                        eq(p.url!!.toASCIIString()),
                        eq(p.hasToBeDeleted),
                        eq(p.type),
                        argThat { map { it.id }.containsAll(listOf(newTagId, oldTagId)) && size == 2 },
                        eq(savedCover)
                ))
                        .thenReturn(podcast.toMono())

                /* When */
                StepVerifier.create(service.save(p))
                        /* Then */
                        .expectSubscription()
                        .assertNext { assertThat(it).isSameAs(podcast) }
                        .verifyComplete()
            }

        }

    }

    @Nested
    @DisplayName("should update podcast")
    @Suppress("UnassignedFluxMonoInstance")
    inner class ShouldUpdatePodcast {

        private val id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729")

        private val podcastForUpdate = PodcastForUpdate(
                id = id,
                title = "Podcast title",
                url = URI("https://foo.bar.com/app/file.rss"),
                hasToBeDeleted = true,
                tags = setOf(),
                cover = CoverForCreation(url = URI("https://external.domain.tld/cover.png"), height = 200, width = 200)
        )

        @BeforeEach
        fun beforeEach() {
            Mockito.reset(tagRepository, coverRepository, fileService)
        }

        @Test
        fun `with same data`() {
            /* Given */
            val p = podcast
            whenever(repository.findById(p.id)).thenReturn(podcast.toMono())
            whenever(repository.update(
                    id = eq(p.id),
                    title = eq(p.title),
                    url = eq(p.url),
                    hasToBeDeleted = eq(p.hasToBeDeleted),
                    tags = argThat { isEmpty() },
                    cover = argThat {
                        id == UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea") &&
                                url == java.net.URI("https://external.domain.tld/cover.png") &&
                                height == 200 && width == 200
                    }
            )).thenReturn(p.toMono())

            /* When */
            StepVerifier.create(service.update(podcastForUpdate))
                    /* Then */
                    .expectSubscription()
                    .expectNext(p)
                    .verifyComplete()

            verify(tagRepository, never()).save(any())
            verify(coverRepository, never()).save(any())
            verify(fileService, never()).downloadPodcastCover(any())
            verify(fileService, never()).movePodcast(any(), any())
        }

        @Nested
        @DisplayName("with modification on tags")
        inner class WithModificationOnTags {

            private val newTagForCreation = TagForCreation(id = null, name = "Foo")
            private val newTagsInDb = Tag(UUID.randomUUID(), newTagForCreation.name)

            private val oldTagForCreation = TagForCreation(id = UUID.randomUUID(), name = "Bar")
            private val oldTagInDb = Tag(oldTagForCreation.id!!, oldTagForCreation.name)

            @Test
            fun `with only one new tag`() {
                /* Given */
                val pToUpdate = podcastForUpdate.copy(tags = setOf(newTagForCreation))
                val p = podcast.copy(tags = setOf(newTagsInDb))

                whenever(tagRepository.save(newTagForCreation.name)).thenReturn(newTagsInDb.toMono())
                whenever(repository.findById(p.id)).thenReturn(podcast.toMono())
                whenever(repository.update(
                        id = eq(p.id),
                        title = eq(p.title),
                        url = eq(p.url),
                        hasToBeDeleted = eq(p.hasToBeDeleted),
                        tags = argThat { contains(newTagsInDb) && size == 1 },
                        cover = argThat {
                            id == UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea") &&
                                    url == java.net.URI("https://external.domain.tld/cover.png") &&
                                    height == 200 && width == 200
                        }
                )).thenReturn(p.toMono())

                /* When */
                StepVerifier.create(service.update(pToUpdate))
                        /* Then */
                        .expectSubscription()
                        .expectNext(p)
                        .verifyComplete()

                verify(tagRepository, times(1)).save(any())
            }

            @Test
            fun `with one new tag and one old tag`() {
                /* Given */
                val pToUpdate = podcastForUpdate.copy(tags = setOf(newTagForCreation, oldTagForCreation))
                val p = podcast.copy(tags = setOf(newTagsInDb, oldTagInDb))

                whenever(tagRepository.save(newTagForCreation.name)).thenReturn(newTagsInDb.toMono())
                whenever(repository.findById(p.id)).thenReturn(podcast.toMono())
                whenever(repository.update(
                        id = eq(p.id),
                        title = eq(p.title),
                        url = eq(p.url),
                        hasToBeDeleted = eq(p.hasToBeDeleted),
                        tags = argThat { containsAll(listOf(newTagsInDb, oldTagInDb)) && size == 2 },
                        cover = any()
                )).thenReturn(p.toMono())

                /* When */
                StepVerifier.create(service.update(pToUpdate))
                        /* Then */
                        .expectSubscription()
                        .expectNext(p)
                        .verifyComplete()

                verify(tagRepository, times(1)).save(any())
            }

            @Test
            fun `with only one old tag`() {
                /* Given */
                val pToUpdate = podcastForUpdate.copy(tags = setOf(oldTagForCreation))
                val p = podcast.copy(tags = setOf(oldTagInDb))

                whenever(tagRepository.save(newTagForCreation.name)).thenReturn(newTagsInDb.toMono())
                whenever(repository.findById(p.id)).thenReturn(podcast.toMono())
                whenever(repository.update(
                        id = eq(p.id),
                        title = eq(p.title),
                        url = eq(p.url),
                        hasToBeDeleted = eq(p.hasToBeDeleted),
                        tags = argThat { containsAll(listOf(oldTagInDb)) && size == 1 },
                        cover = any()
                )).thenReturn(p.toMono())

                /* When */
                StepVerifier.create(service.update(pToUpdate))
                        /* Then */
                        .expectSubscription()
                        .expectNext(p)
                        .verifyComplete()

                verify(tagRepository, never()).save(any())
            }

            @Test
            fun `with many new and many old tags`() {
                /* Given */
                val tagsName = listOf("Foo", "Bar", "One", "Another", "Tags")
                val listOfNewTags = tagsName
                        .map { "$it.new" }
                        .map { TagForCreation(id = null, name = it) }
                val listOfOldTags = tagsName
                        .map { "$it.old" }
                        .map { TagForCreation(id = UUID.randomUUID(), name = it) }

                val allTagsInDb = (listOfNewTags + listOfOldTags)
                        .map { Tag(it.id ?: UUID.randomUUID(), it.name) }

                val pToUpdate = podcastForUpdate.copy(tags = listOfNewTags + listOfOldTags)
                val p = podcast.copy(tags = allTagsInDb)

                whenever(tagRepository.save(argThat { this in listOfNewTags.map { it.name } }))
                        .then { allTagsInDb.first() { t -> t.name == it.getArgument<String>(0) }.toMono() }
                whenever(repository.findById(p.id)).thenReturn(podcast.toMono())
                whenever(repository.update(
                        id = eq(p.id),
                        title = eq(p.title),
                        url = eq(p.url),
                        hasToBeDeleted = eq(p.hasToBeDeleted),
                        tags = argThat { containsAll(allTagsInDb) && size == allTagsInDb.size },
                        cover = any()
                )).thenReturn(p.toMono())

                /* When */
                StepVerifier.create(service.update(pToUpdate))
                        /* Then */
                        .expectSubscription()
                        .expectNext(p)
                        .verifyComplete()

                verify(tagRepository, times(listOfNewTags.size)).save(any())
            }

            @AfterEach
            fun afterEach() {
                verify(coverRepository, never()).save(any())
                verify(fileService, never()).downloadPodcastCover(any())
                verify(fileService, never()).movePodcast(any(), any())
            }

        }

        @Nested
        @DisplayName("with modification on cover")
        inner class WithModificationOnCover {

            @Test
            fun `and do nothing if new cover url is relative`() {
                /* Given */
                val pToUpdate = podcastForUpdate.copy(
                        cover = CoverForCreation(200, 200, URI("/api/v1/cover.png"))
                )
                val p = podcast

                whenever(repository.findById(p.id)).thenReturn(podcast.toMono())
                whenever(repository.update(
                        id = eq(p.id),
                        title = eq(p.title),
                        url = eq(p.url),
                        hasToBeDeleted = eq(p.hasToBeDeleted),
                        tags = any(),
                        cover = argThat {
                            id == UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea") &&
                                    url == URI("https://external.domain.tld/cover.png") &&
                                    height == 200 && width == 200
                        }
                )).thenReturn(p.toMono())

                /* When */
                StepVerifier.create(service.update(pToUpdate))
                        /* Then */
                        .expectSubscription()
                        .expectNext(p)
                        .verifyComplete()

                verify(coverRepository, never()).save(any())
            }

            @Test
            fun `and do nothing if new cover url is same as old cover`() {
                /* Given */
                val p = podcast
                val pToUpdate = podcastForUpdate.copy(
                        cover = CoverForCreation(200, 200, p.cover.url)
                )

                whenever(repository.findById(p.id)).thenReturn(podcast.toMono())
                whenever(repository.update(
                        id = eq(p.id),
                        title = eq(p.title),
                        url = eq(p.url),
                        hasToBeDeleted = eq(p.hasToBeDeleted),
                        tags = any(),
                        cover = argThat {
                            id == UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea") &&
                                    url == URI("https://external.domain.tld/cover.png") &&
                                    height == 200 && width == 200
                        }
                )).thenReturn(p.toMono())

                /* When */
                StepVerifier.create(service.update(pToUpdate))
                        /* Then */
                        .expectSubscription()
                        .expectNext(p)
                        .verifyComplete()

                verify(coverRepository, never()).save(any())
            }

            @Test
            fun `and save new cover`() {
                /* Given */
                val newCover = CoverForCreation(200, 200, URI("http://foo.bar.com/image.png"))
                val coverInDb = Cover(UUID.randomUUID(), newCover.url, newCover.height, newCover.width)
                val pToUpdate = podcastForUpdate.copy(cover = newCover)
                val p = podcast

                whenever(repository.findById(p.id)).thenReturn(podcast.toMono())
                whenever(coverRepository.save(newCover)).thenReturn(coverInDb.toMono())
                whenever(fileService.downloadPodcastCover(argThat { title == p.title && cover.url == newCover.url }))
                        .thenReturn(Mono.empty())
                whenever(repository.update(
                        id = eq(p.id),
                        title = eq(p.title),
                        url = eq(p.url),
                        hasToBeDeleted = eq(p.hasToBeDeleted),
                        tags = any(),
                        cover = eq(coverInDb)
                )).thenReturn(p.toMono())

                /* When */
                StepVerifier.create(service.update(pToUpdate))
                        /* Then */
                        .expectSubscription()
                        .expectNext(p)
                        .verifyComplete()

                verify(coverRepository, times(1)).save(any())
                verify(fileService, times(1)).downloadPodcastCover(any())
            }



            @AfterEach
            fun afterEach() {
                verify(tagRepository, never()).save(any())
                verify(fileService, never()).movePodcast(any(), any())
            }
        }

        @Nested
        @DisplayName("with modification on title")
        inner class WithModificationOnTitle {

            @Test
            fun `and should trigger a move of the folder`() {
                /* Given */
                val p = podcast.copy(title = "oldName")
                val pToUpdate = podcastForUpdate.copy(title = "newName")
                val pAfterUpdate = p.copy(title = "newName")
                whenever(repository.findById(p.id)).thenReturn(p.toMono())
                whenever(repository.update(
                        id = eq(p.id),
                        title = eq(pToUpdate.title),
                        url = eq(p.url),
                        hasToBeDeleted = eq(p.hasToBeDeleted),
                        tags = any(),
                        cover = any()
                )).thenReturn(pAfterUpdate.toMono())
                whenever(fileService.movePodcast(p, pToUpdate.title)).thenReturn(Mono.empty())

                /* When */
                StepVerifier.create(service.update(pToUpdate))
                        /* Then */
                        .expectSubscription()
                        .expectNext(pAfterUpdate)
                        .verifyComplete()

                verify(tagRepository, never()).save(any())
                verify(coverRepository, never()).save(any())
                verify(fileService, never()).downloadPodcastCover(any())
                verify(fileService, times(1)).movePodcast(p, pToUpdate.title)
            }

        }
    }

    @Nested
    @DisplayName("should delete")
    inner class ShouldDelete {

        @BeforeEach
        fun beforeEach() = Mockito.reset(repository, fileService)

        @Test
        fun `a podcast which has to be deleted`() {
            /* Given */
            val id = UUID.randomUUID()
            val information = DeletePodcastInformation(id, "foo")
            whenever(repository.deleteById(id)).thenReturn(information.toMono())
            whenever(fileService.deletePodcast(information)).thenReturn(Mono.empty())

            /* When */
            StepVerifier.create(service.deleteById(id))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            verify(fileService, times(1)).deletePodcast(information)
        }

        @Test
        fun `a podcast which should not be deleted`() {
            /* Given */
            val id = UUID.randomUUID()
            whenever(repository.deleteById(id)).thenReturn(Mono.empty())

            /* When */
            StepVerifier.create(service.deleteById(id))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            verify(fileService, never()).deletePodcast(any())
        }
    }
}

private fun CoverForCreation.toPodcastCover() = Cover(UUID.randomUUID(), url = url, height = height, width = width)
