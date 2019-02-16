package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.business.stats.NumberOfItemByDateWrapper
import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.DbSetupTracker
import com.ninja_squad.dbsetup.destination.DataSourceDestination
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.DELETE_ALL
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.INSERT_ITEM_DATA
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.INSERT_PODCAST_DATA
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import reactor.test.StepVerifier
import java.time.LocalDate
import java.util.*
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
                        Assertions.assertThat(it.id).isEqualTo(id)
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

        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)
            println(dataSource.connection.metaData.url)
            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Test
        fun `by pubDate`() {
            /* Given */
            /* When */
            StepVerifier.create(repository.findStatByPubDate(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), 13))
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
            StepVerifier.create(repository.findStatByDownloadDate(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), 13))
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
            StepVerifier.create(repository.findStatByCreationDate(UUID.fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), 13))
                    /* Then */
                    .expectSubscription()
                    .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusWeeks(1), 1))
                    .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusWeeks(2), 1))
                    .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusMonths(2), 2))
                    .verifyComplete()
        }



    }

}
