package com.github.davinkevin.podcastserver.podcast

import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.DbSetupTracker
import com.ninja_squad.dbsetup.destination.DataSourceDestination
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.DELETE_ALL
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

    @BeforeEach
    fun prepare() {
        val operation = sequenceOf(DELETE_ALL, INSERT_PODCAST_DATA)
        val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

        dbSetupTracker.launchIfNecessary(dbSetup)
    }

    @Nested
    @DisplayName("Should find by id")
    inner class ShouldFindById {

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

}
