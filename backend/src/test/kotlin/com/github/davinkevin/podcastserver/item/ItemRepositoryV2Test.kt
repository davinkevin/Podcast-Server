package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.entity.Status
import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.DbSetupTracker
import com.ninja_squad.dbsetup.destination.DataSourceDestination
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.DELETE_ALL
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.INSERT_ITEM_DATA
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import reactor.test.StepVerifier
import java.time.OffsetDateTime.now
import java.util.*
import javax.sql.DataSource
import com.github.davinkevin.podcastserver.item.ItemRepositoryV2 as ItemRepository

/**
 * Created by kevin on 2019-02-09
 */

@JooqTest
@Import(ItemRepository::class)
class ItemRepositoryV2Test {

    @Autowired lateinit var query: DSLContext
    @Autowired lateinit var repository: ItemRepository
    @Autowired lateinit var dataSource: DataSource

    private val dbSetupTracker = DbSetupTracker()

    @BeforeEach
    fun prepare() {
        val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
        val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

        dbSetupTracker.launchIfNecessary(dbSetup)
    }

    @Nested
    @DisplayName("Should find by id")
    inner class ShouldFindById {

        @BeforeEach
        fun beforeEach() {
            dbSetupTracker.skipNextLaunch()
        }

        @Test
        fun `and return one matching element`() {
            /* Given */
            val id = UUID.fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")

            /* When */
            StepVerifier.create(repository.findById(id))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isEqualTo(id)
                    }
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


    @Test
    fun `should find all to delete`() {
        /* Given */
        dbSetupTracker.skipNextLaunch()
        val today = now()

        /* When */
        StepVerifier.create(repository.findAllToDelete(today))
                /* Then */
                .assertNext {
                    assertThat(it.id).isEqualTo(UUID.fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"))
                }
                .verifyComplete()
    }

    @Test
    fun `should delete`() {
        /* Given */
        val item1 = UUID.fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb")
        val item2 = UUID.fromString("817a4626-6fd2-457e-8d27-69ea5acdc828")
        val item3 = UUID.fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")

        /* When */
        StepVerifier.create(repository.deleteById(listOf(item1, item2, item3)))
                .expectSubscription()
                /* Then */
                .verifyComplete()

        val items = query.select(ITEM.ID).from(ITEM).fetch { it[ITEM.ID] }
        assertThat(items).hasSize(4).contains(
                UUID.fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"),
                UUID.fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"),
                UUID.fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"),
                UUID.fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")
        )
    }

    @Test
    fun `should update as deleted`() {
        /* Given */
        val item1 = UUID.fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb")
        val item2 = UUID.fromString("817a4626-6fd2-457e-8d27-69ea5acdc828")
        val item3 = UUID.fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")
        val ids = listOf(item1, item2, item3)
        /* When */
        StepVerifier.create(repository.updateAsDeleted(ids))
                /* Then */
                .expectSubscription()
                .then {
                    val items = query.selectFrom(ITEM).where(ITEM.ID.`in`(ids)).fetch()
                    assertThat(items).allSatisfy {
                        assertThat(it.status).isEqualTo(Status.DELETED.toString())
                        assertThat(it.fileName).isNull()
                    }
                }
                .verifyComplete()
    }
}
