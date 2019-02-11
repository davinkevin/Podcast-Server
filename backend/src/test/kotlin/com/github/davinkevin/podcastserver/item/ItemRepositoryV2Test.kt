package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.DbSetupTracker
import com.ninja_squad.dbsetup.destination.DataSourceDestination
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.DELETE_ALL
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.INSERT_ITEM_DATA
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
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
    @Autowired lateinit var itemRepository: ItemRepository
    @Autowired lateinit var dataSource: DataSource

    private val dbSetupTracker = DbSetupTracker()

    @BeforeEach
    fun prepare() {
        val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
        val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

        dbSetupTracker.launchIfNecessary(dbSetup)
    }

    @Test
    fun `should find all to delete`() {
        /* Given */
        dbSetupTracker.skipNextLaunch()
        val today = now()

        /* When */
        StepVerifier.create(itemRepository.findAllToDelete(today))
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
        StepVerifier.create(itemRepository.deleteById(listOf(item1, item2, item3)))
                .expectSubscription()
                /* Then */
                .then {
                    val items = query.select(ITEM.ID).from(ITEM).fetch { it[ITEM.ID] }
                    assertThat(items).hasSize(4).contains(
                            UUID.fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"),
                            UUID.fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"),
                            UUID.fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"),
                            UUID.fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")
                    )
                }
                .verifyComplete()
    }
}