package com.github.davinkevin.podcastserver.tag

import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.DbSetupTracker
import com.ninja_squad.dbsetup.destination.DataSourceDestination
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import javax.sql.DataSource
import com.github.davinkevin.podcastserver.tag.TagRepositoryV2 as TagRepository
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import lan.dk.podcastserver.repository.DatabaseConfigurationTest
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.*
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import java.util.*

/**
 * Created by kevin on 2019-03-24
 */
@JooqTest
@Import(TagRepository::class)
class TagRepositoryV2Test {

    @Autowired lateinit var query: DSLContext
    @Autowired lateinit var repository: TagRepository
    @Autowired lateinit var dataSource: DataSource

    private val dbSetupTracker = DbSetupTracker()

    @BeforeEach
    fun beforeEach() {
        val operation = sequenceOf(DELETE_ALL, INSERT_TAG_DATA)
        val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

        dbSetupTracker.launchIfNecessary(dbSetup)
    }

    @Nested
    @DisplayName("Should find by id")
    inner class ShouldFindById {

        @BeforeEach
        fun beforeEach() = dbSetupTracker.skipNextLaunch()

        @Test
        fun `and return one matching element`() {
            /* Given */
            val id = UUID.fromString("eb355a23-e030-4966-b75a-b70881a8bd08")

            /* When */
            StepVerifier.create(repository.findById(id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(Tag(id, "Foo"))
                    .verifyComplete()
        }

        @Test
        fun `and return empty mono if not find by id`() {
            /* Given */
            val id = UUID.fromString("98b33370-a976-4e4d-9ab8-57d47241e693")

            /* When */
            StepVerifier.create(repository.findById(id))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

    }

    @Nested
    @DisplayName("Should find by name")
    inner class ShouldFindByNameLike {

        @BeforeEach
        fun beforeEach() = dbSetupTracker.skipNextLaunch()

        @Test
        fun `with with insensitive case results`() {
            /* Given */

            /* When */
            StepVerifier.create(repository.findByNameLike("bar"))
                    /* Then */
                    .expectSubscription()
                    .expectNext(Tag(UUID.fromString("ad109389-9568-4bdb-ae61-6f26bf6ffdf6"), "Another Bar"))
                    .expectNext(Tag(UUID.fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "bAr"))
                    .verifyComplete()
        }

        @Test
        fun `without any match`() {
            /* Given */
            /* When */
            StepVerifier.create(repository.findByNameLike("boo"))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

    }
}
