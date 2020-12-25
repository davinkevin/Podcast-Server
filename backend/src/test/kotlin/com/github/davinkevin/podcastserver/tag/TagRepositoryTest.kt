package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.INSERT_TAG_DATA
import com.github.davinkevin.podcastserver.database.tables.Tag.TAG
import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.DbSetupTracker
import com.ninja_squad.dbsetup.destination.DataSourceDestination
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import com.github.davinkevin.podcastserver.DELETE_ALL
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
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

/**
 * Created by kevin on 2019-03-24
 */
@JooqTest
@Import(TagRepository::class)
class TagRepositoryTest(
    @Autowired val query: DSLContext,
    @Autowired val repository: TagRepository,
    @Autowired val dataSource: DataSource
) {

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

    @Nested
    @DisplayName("Should save ")
    inner class ShouldSave {

        @Test
        fun `an item with just name`() {
            /* Given */
            val name = "a_wonderful_tag_name"
            /* When */
            StepVerifier.create(repository.save(name))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isNotNull()
                        assertThat(it.name).isEqualTo(name)

                        val tagRecord = query.selectFrom(TAG).where(TAG.NAME.eq(name)).fetchOne()
                        val numberOfTag = query.selectCount().from(TAG).fetchOne(count())
                        assertThat(tagRecord?.id).isEqualTo(it.id)
                        assertThat(numberOfTag).isEqualTo(4)
                    }
                    .verifyComplete()

        }

        @Test
        fun `an item already existing`() {
            /* Given */
            val name = "Foo"
            /* When */
            StepVerifier.create(repository.save(name))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isNotNull()
                        assertThat(it.name).isEqualTo(name)

                        val tagRecord = query.selectFrom(TAG).where(TAG.NAME.eq(name)).fetchOne()
                        val numberOfTag = query.selectCount().from(TAG).fetchOne(count())
                        assertThat(tagRecord?.id).isEqualTo(it.id)
                        assertThat(numberOfTag).isEqualTo(3)
                    }
                    .verifyComplete()

        }

    }
}
