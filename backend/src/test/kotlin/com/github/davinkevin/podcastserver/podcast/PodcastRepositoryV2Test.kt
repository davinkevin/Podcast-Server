package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.tag.Tag
import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.DbSetupTracker
import com.ninja_squad.dbsetup.destination.DataSourceDestination
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.DELETE_ALL
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.INSERT_ITEM_DATA
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.INSERT_PODCAST_DATA
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import reactor.test.StepVerifier
import java.net.URI
import java.time.LocalDate
import java.util.*
import java.util.UUID.fromString
import javax.sql.DataSource
import com.github.davinkevin.podcastserver.podcast.PodcastRepositoryV2 as PodcastRepository

/**
 * Created by kevin on 2019-02-16
 */
@JooqTest
@Import(PodcastRepository::class)
class PodcastRepositoryV2Test {

    @Autowired lateinit var repository: PodcastRepository
    @Autowired lateinit var dataSource: DataSource

    private val dbSetupTracker = DbSetupTracker()

    @Nested
    @DisplayName("Should find by id")
    inner class ShouldFindById {

        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_PODCAST_DATA)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Test
        fun `and return one matching element`() {
            /* Given */
            val id = UUID.fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d")

            /* When */
            StepVerifier.create(repository.findById(id))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isEqualTo(id)
                        assertThat(it.title).isEqualTo("Geek Inc HD")
                        assertThat(it.url).isEqualTo("http://fake.url.com/rss")
                        assertThat(it.hasToBeDeleted).isEqualTo(true)
                        assertThat(it.type).isEqualTo("Youtube")
                        assertThat(it.tags).containsOnly(
                                Tag(fromString("df801a7a-5630-4442-8b83-0cb36ae94981"),"Geek"),
                                Tag(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"),"Studio Renegade")
                        )
                        assertThat(it.cover.id).isEqualTo(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"))
                        assertThat(it.cover.url).isEqualTo(URI("http://fake.url.com/appload/cover.png"))
                        assertThat(it.cover.width).isEqualTo(100)
                        assertThat(it.cover.height).isEqualTo(100)
                    }
                    .verifyComplete()
        }

        @Test
        fun `and don't return any element`() {
            /* Given */
            val id = UUID.fromString("ef85dcd3-758c-573f-a8fc-b82104762d9d")

            /* When */
            StepVerifier.create(repository.findById(id))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

    }

    @Nested
    @DisplayName("should find stats")
    inner class ShouldFindStats {

        @Nested
        @DisplayName("by podcast")
        inner class ByPodcast {

            @BeforeEach
            fun prepare() {
                val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
                val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)
                dbSetupTracker.launchIfNecessary(dbSetup)
            }

            @Test
            fun `by pubDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByPodcastIdAndPubDate(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), 13))
                        /* Then */
                        .expectSubscription()
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusDays(1), 2))
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1))
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusYears(1), 1))
                        .verifyComplete()
            }

            @Test
            fun `by downloadDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByPodcastIdAndDownloadDate(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), 13))
                        /* Then */
                        .expectSubscription()
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now(), 1))
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1))
                        .verifyComplete()
            }

            @Test
            fun `by creationDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByPodcastIdAndCreationDate(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), 13))
                        /* Then */
                        .expectSubscription()
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusWeeks(1), 1))
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusWeeks(2), 1))
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusMonths(2), 2))
                        .verifyComplete()
            }

        }

        @Nested
        @DisplayName("globally")
        inner class Globally {

            @BeforeEach
            fun prepare() {
                val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
                val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)
                dbSetupTracker.launchIfNecessary(dbSetup)
            }

            @Test
            fun `by pubDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByTypeAndPubDate(13))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.type).isEqualTo("RSS")
                            assertThat(it.values).contains(
                                    NumberOfItemByDateWrapper(LocalDate.now(), 1),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(30), 1)
                            )
                        }
                        .assertNext {
                            assertThat(it.type).isEqualTo("YOUTUBE")
                            assertThat(it.values).contains(
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(1), 2),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusYears(1), 1)
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `by downloadDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByTypeAndDownloadDate(13))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.type).isEqualTo("YOUTUBE")
                            assertThat(it.values).contains(
                                    NumberOfItemByDateWrapper(LocalDate.now(), 1),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1)
                            )
                        }
                        .assertNext {
                            assertThat(it.type).isEqualTo("RSS")
                            assertThat(it.values).contains(
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1)
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `by creationDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByTypeAndCreationDate(13))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.type).isEqualTo("YOUTUBE")
                            assertThat(it.values).contains(
                                    NumberOfItemByDateWrapper(LocalDate.now().minusWeeks(1), 1),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusMonths(2), 1)
                            )
                        }
                        .verifyComplete()
            }


        }


    }

}
