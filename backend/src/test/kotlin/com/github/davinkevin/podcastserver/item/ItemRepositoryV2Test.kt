package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.database.Tables.COVER
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Status.*
import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.DbSetupTracker
import com.ninja_squad.dbsetup.Operations.insertInto
import com.ninja_squad.dbsetup.destination.DataSourceDestination
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
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
import java.net.URI
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.*
import java.util.*
import java.util.UUID.fromString
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

    @Nested
    @DisplayName("Should find")
    inner class ShouldFindById {

        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
            dbSetupTracker.skipNextLaunch()
        }

        @Test
        fun `by id and return one matching element`() {
            /* Given */
            val id = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")

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
        fun `by id and return empty mono if not find by id`() {
            /* Given */
            val id = fromString("98b33370-a976-4e4d-9ab8-57d47241e693")

            /* When */
            StepVerifier.create(repository.findById(id))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }
    }

    @Nested
    @DisplayName("Should find all")
    inner class ShouldFindAll {

        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Test
        fun `to delete`() {
            /* Given */
            dbSetupTracker.skipNextLaunch()
            val today = now()

            /* When */
            StepVerifier.create(repository.findAllToDelete(today))
                    /* Then */
                    .assertNext {
                        assertThat(it.id).isEqualTo(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"))
                    }
                    .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should delete")
    inner class ShouldDelete {

        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Test
        fun `by id`() {
            /* Given */
            val item1 = fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb")
            val item2 = fromString("817a4626-6fd2-457e-8d27-69ea5acdc828")
            val item3 = fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")

            /* When */
            StepVerifier.create(repository.deleteById(listOf(item1, item2, item3)))
                    .expectSubscription()
                    /* Then */
                    .verifyComplete()

            val items = query.select(ITEM.ID).from(ITEM).fetch { it[ITEM.ID] }
            assertThat(items).hasSize(4).contains(
                    fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"),
                    fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"),
                    fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"),
                    fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")
            )
        }
    }

    @Nested
    @DisplayName("Should update")
    inner class ShouldUpdateAsDeleted {

        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Test
        fun `as deleted`() {
            /* Given */
            val item1 = fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb")
            val item2 = fromString("817a4626-6fd2-457e-8d27-69ea5acdc828")
            val item3 = fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")
            val ids = listOf(item1, item2, item3)
            /* When */
            StepVerifier.create(repository.updateAsDeleted(ids))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            val items = query.selectFrom(ITEM).where(ITEM.ID.`in`(ids)).fetch()
            assertThat(items).allSatisfy {
                assertThat(it.status).isEqualTo(DELETED.toString())
                assertThat(it.fileName).isNull()
            }
        }

    }

    @Nested
    @DisplayName("Should reset")
    inner class ShouldReset {

        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Test
        fun `by Id`() {
            /* Given */
            val id = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")
            /* When */
            StepVerifier.create(repository.resetById(id))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isEqualTo(id)
                        assertThat(it.title).isEqualTo("Geek INC 126")
                        assertThat(it.url).isEqualTo("http://fakeurl.com/geekinc.126.mp3")
                        assertThat(it.fileName).isEqualTo(null)
                        assertThat(it.fileName).isEqualTo(null)
                        assertThat(it.podcast).isEqualTo(PodcastForItem(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss"))
                        assertThat(it.status).isEqualTo(NOT_DOWNLOADED)
                        assertThat((it.downloadDate == null)).isEqualTo(true)
                    }
                    .then {
                        val numberOfFail = query.selectFrom(ITEM).where(ITEM.ID.eq(id)).fetchOne().numberOfFail
                        assertThat(numberOfFail).isEqualTo(0)
                    }
                    .verifyComplete()
        }

    }

    @Nested
    @DisplayName("should find if item has to be deleted")
    inner class HasToBeDeleted {


        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
            dbSetupTracker.skipNextLaunch()
        }

        @Test
        fun `and return true because its parent podcast has to`() {
            /* Given */
            val id = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")
            /* When */
            StepVerifier.create(repository.hasToBeDeleted(id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(true)
                    .verifyComplete()
        }

        @Test
        fun `and return false because its parent podcast hasn't`() {
            /* Given */
            val id = fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")
            /* When */
            StepVerifier.create(repository.hasToBeDeleted(id))
                    /* Then */
                    .expectSubscription()
                    .expectNext(false)
                    .verifyComplete()
        }


    }

    @Nested
    @DisplayName("should search with pagination")
    inner class ShouldSearchWithPagination {

        private val coverDb = insertInto("COVER")
                .columns("ID", "URL", "WIDTH", "HEIGHT")
                .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/Appload/cover.png", 100, 100)
                .values(fromString("4b240b0a-516b-42e9-b9fc-e49b5f868045"), "http://fake.url.com/geekinchd/cover.png", 100, 100)
                .values(fromString("a8eb1ea2-354c-4a8e-931a-dc0286a2a66e"), "http://fake.url.com/foopodcast/cover.png", 100, 100)
                .values(fromString("8eac2413-3732-4c40-9c80-03e166dba3f0"), "http://fake.url.com/otherpodcast/cover.png", 100, 100)
                .build()!!

        private val podcastDb = insertInto("PODCAST")
                .columns("ID", "TITLE", "URL", "TYPE", "HAS_TO_BE_DELETED")
                .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Appload", "http://fake.url.com/appload/rss", "YOUTUBE", true)
                .values(fromString("ccb75276-7a8c-4da9-b4fd-27ccec075c65"), "Geek Inc HD", "http://fake.url.com/geekinchd/rss", "YOUTUBE", true)
                .values(fromString("cfb8c605-7e10-43b1-9b40-41ee8b5b13d3"), "Foo podcast", "http://fake.url.com/foo/rss", "YOUTUBE", true)
                .values(fromString("4dc2ccef-42ab-4733-8945-e3f2849b8083"), "Other Podcast", "http://fake.url.com/other/rss", "YOUTUBE", true)
                .build()!!

        private val itemsDb = insertInto("ITEM")
                .columns("ID", "TITLE", "URL", "FILE_NAME", "PODCAST_ID", "STATUS", "PUB_DATE", "DOWNLOAD_DATE", "CREATION_DATE", "NUMBER_OF_FAIL", "COVER_ID", "DESCRIPTION").apply {
                    val max = 50
                    (1..max).forEach { val idx = max - it + 1; values(UUID.randomUUID(), "Appload $idx", "http://fakeurl.com/appload.$idx.mp3", "appload.$idx.mp3", fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), FINISH, now().minusDays(it.toLong()).format(formatter), now().minusDays(it.toLong()+1).format(formatter), now().minusDays(15.toLong()+2).format(formatter), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc") }
                    (1..max).forEach { val idx = max - it + 1; values(UUID.randomUUID(), "Geek Inc HD $idx", "http://fakeurl.com/geekinchd.$idx.mp3", "geekinchd.$idx.mp3", fromString("ccb75276-7a8c-4da9-b4fd-27ccec075c65"), FINISH, now().minusDays(it.toLong()).format(formatter), now().minusDays(it.toLong()+1).format(formatter), now().minusDays(15.toLong()+2).format(formatter), 0, fromString("4b240b0a-516b-42e9-b9fc-e49b5f868045"), "desc") }
                    (1..max).forEach { val idx = max - it + 1; values(UUID.randomUUID(), "Foo podcast $idx", "http://fakeurl.com/foo.$idx.mp3", "foo.$idx.mp3", fromString("cfb8c605-7e10-43b1-9b40-41ee8b5b13d3"), FINISH, now().minusDays(it.toLong()).format(formatter), now().minusDays(it.toLong()+1).format(formatter), now().minusDays(15.toLong()+2).format(formatter), 0, fromString("a8eb1ea2-354c-4a8e-931a-dc0286a2a66e"), "desc") }
                    (1..max).forEach { val idx = max - it + 1; values(UUID.randomUUID(), "Other Podcast $idx", "http://fakeurl.com/other.$idx.mp3", "other.$idx.mp3", fromString("4dc2ccef-42ab-4733-8945-e3f2849b8083"), NOT_DOWNLOADED, now().minusDays(it.toLong()).format(formatter), now().minusDays(it.toLong()+1).format(formatter), now().minusDays(15.toLong()+2).format(formatter), 0, fromString("8eac2413-3732-4c40-9c80-03e166dba3f0"), "desc") }
                }.build()!!

        private val tagsDb = insertInto("TAG")
                .columns("ID", "NAME")
                .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "T1")
                .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "T2")
                .values(fromString("6936b895-0de6-43f6-acaa-678511d3c37b"), "T3")
                .values(fromString("4cff2eb7-6398-43cf-96b7-f5699377fdb4"), "T4")
                .build()!!

        private val podcastToTags = insertInto("PODCAST_TAGS")
                .columns("PODCASTS_ID", "TAGS_ID")
                .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))
                .values(fromString("ccb75276-7a8c-4da9-b4fd-27ccec075c65"), fromString("6936b895-0de6-43f6-acaa-678511d3c37b"))
                .values(fromString("4dc2ccef-42ab-4733-8945-e3f2849b8083"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                .build()

        @BeforeEach
        fun prepare() {

            val operation = sequenceOf(DELETE_ALL, coverDb, podcastDb, itemsDb, tagsDb, podcastToTags)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
            dbSetupTracker.skipNextLaunch()
        }

        @Nested
        @DisplayName("with no tags and no specific statuses")
        inner class WithNoTagsAndNoStatutes {

            @Test
            fun `for first page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf(), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isTrue()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(0)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(200)
                            assertThat(it.totalPages).isEqualTo(17)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 50", "Geek Inc HD 50", "Foo podcast 50", "Other Podcast 50",
                                    "Appload 49", "Geek Inc HD 49", "Foo podcast 49", "Other Podcast 49",
                                    "Appload 48", "Geek Inc HD 48", "Foo podcast 48", "Other Podcast 48"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for second page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(1, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf(), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(1)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(200)
                            assertThat(it.totalPages).isEqualTo(17)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 47", "Geek Inc HD 47", "Foo podcast 47", "Other Podcast 47",
                                    "Appload 46", "Geek Inc HD 46", "Foo podcast 46", "Other Podcast 46",
                                    "Appload 45", "Geek Inc HD 45", "Foo podcast 45", "Other Podcast 45"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for before last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(15, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf(), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(15)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(200)
                            assertThat(it.totalPages).isEqualTo(17)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 5", "Geek Inc HD 5", "Foo podcast 5", "Other Podcast 5",
                                    "Appload 4", "Geek Inc HD 4", "Foo podcast 4", "Other Podcast 4",
                                    "Appload 3", "Geek Inc HD 3", "Foo podcast 3", "Other Podcast 3"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(16, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf(), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(8)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isTrue()
                            assertThat(it.number).isEqualTo(16)
                            assertThat(it.numberOfElements).isEqualTo(8)
                            assertThat(it.totalElements).isEqualTo(200)
                            assertThat(it.totalPages).isEqualTo(17)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 2", "Geek Inc HD 2", "Foo podcast 2", "Other Podcast 2",
                                    "Appload 1", "Geek Inc HD 1", "Foo podcast 1", "Other Podcast 1"
                            )
                        }
                        .verifyComplete()
            }


        }

        @Nested
        @DisplayName("with tags T1 and no specific statuses")
        inner class WithTagsT1AndNoSpecificStatutes {

            @Test
            fun `for first page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T1"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isTrue()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(0)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(100)
                            assertThat(it.totalPages).isEqualTo(9)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 50", "Other Podcast 50",
                                    "Appload 49", "Other Podcast 49",
                                    "Appload 48", "Other Podcast 48",
                                    "Appload 47", "Other Podcast 47",
                                    "Appload 46", "Other Podcast 46",
                                    "Appload 45", "Other Podcast 45"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for second page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(1, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T1"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(1)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(100)
                            assertThat(it.totalPages).isEqualTo(9)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 44", "Other Podcast 44",
                                    "Appload 43", "Other Podcast 43",
                                    "Appload 42", "Other Podcast 42",
                                    "Appload 41", "Other Podcast 41",
                                    "Appload 40", "Other Podcast 40",
                                    "Appload 39", "Other Podcast 39"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for before last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(7, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T1"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(7)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(100)
                            assertThat(it.totalPages).isEqualTo(9)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 8", "Other Podcast 8",
                                    "Appload 7", "Other Podcast 7",
                                    "Appload 6", "Other Podcast 6",
                                    "Appload 5", "Other Podcast 5",
                                    "Appload 4", "Other Podcast 4",
                                    "Appload 3", "Other Podcast 3"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(8, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T1"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(4)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isTrue()
                            assertThat(it.number).isEqualTo(8)
                            assertThat(it.numberOfElements).isEqualTo(4)
                            assertThat(it.totalElements).isEqualTo(100)
                            assertThat(it.totalPages).isEqualTo(9)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 2", "Other Podcast 2", "Appload 1", "Other Podcast 1"
                            )
                        }
                        .verifyComplete()
            }


        }

        @Nested
        @DisplayName("with tags (T1 and T2) and no specific statuses")
        inner class WithTagsT1AndT2AndNoSpecificStatutes {

            @Test
            fun `for first page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T1", "T2"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isTrue()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(0)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 50",
                                    "Appload 49",
                                    "Appload 48",
                                    "Appload 47",
                                    "Appload 46",
                                    "Appload 45",
                                    "Appload 44",
                                    "Appload 43",
                                    "Appload 42",
                                    "Appload 41",
                                    "Appload 40",
                                    "Appload 39"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for second page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(1, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T1", "T2"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(1)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 38",
                                    "Appload 37",
                                    "Appload 36",
                                    "Appload 35",
                                    "Appload 34",
                                    "Appload 33",
                                    "Appload 32",
                                    "Appload 31",
                                    "Appload 30",
                                    "Appload 29",
                                    "Appload 28",
                                    "Appload 27"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for before last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(3, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T1", "T2"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(3)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 14",
                                    "Appload 13",
                                    "Appload 12",
                                    "Appload 11",
                                    "Appload 10",
                                    "Appload 9",
                                    "Appload 8",
                                    "Appload 7",
                                    "Appload 6",
                                    "Appload 5",
                                    "Appload 4",
                                    "Appload 3"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(4, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T1", "T2"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(2)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isTrue()
                            assertThat(it.number).isEqualTo(4)
                            assertThat(it.numberOfElements).isEqualTo(2)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Appload 2", "Appload 1"
                            )
                        }
                        .verifyComplete()
            }


        }

        @Nested
        @DisplayName("with tags (T3) and no specific statuses")
        inner class WithTagsT3AndNoSpecificStatutes {

            @Test
            fun `for first page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T3"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isTrue()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(0)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Geek Inc HD 50", "Geek Inc HD 49", "Geek Inc HD 48", "Geek Inc HD 47",
                                    "Geek Inc HD 46", "Geek Inc HD 45", "Geek Inc HD 44", "Geek Inc HD 43",
                                    "Geek Inc HD 42", "Geek Inc HD 41", "Geek Inc HD 40", "Geek Inc HD 39"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for second page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(1, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T3"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(1)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Geek Inc HD 38", "Geek Inc HD 37", "Geek Inc HD 36", "Geek Inc HD 35",
                                    "Geek Inc HD 34", "Geek Inc HD 33", "Geek Inc HD 32", "Geek Inc HD 31",
                                    "Geek Inc HD 30", "Geek Inc HD 29", "Geek Inc HD 28", "Geek Inc HD 27"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for before last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(3, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T3"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(3)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Geek Inc HD 14", "Geek Inc HD 13", "Geek Inc HD 12", "Geek Inc HD 11",
                                    "Geek Inc HD 10", "Geek Inc HD 9", "Geek Inc HD 8", "Geek Inc HD 7",
                                    "Geek Inc HD 6", "Geek Inc HD 5", "Geek Inc HD 4", "Geek Inc HD 3"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(4, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf("T3"), listOf(), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(2)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isTrue()
                            assertThat(it.number).isEqualTo(4)
                            assertThat(it.numberOfElements).isEqualTo(2)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title))
                                    .contains("Geek Inc HD 2", "Geek Inc HD 1")
                        }
                        .verifyComplete()
            }


        }

        @Nested
        @DisplayName("with no tags and status not downloaded")
        inner class WithNoTagsAndStatusNotDownloaded {

            @Test
            fun `for first page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf(), listOf(NOT_DOWNLOADED), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isTrue()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(0)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Other Podcast 50",
                                    "Other Podcast 49",
                                    "Other Podcast 48",
                                    "Other Podcast 47",
                                    "Other Podcast 46",
                                    "Other Podcast 45",
                                    "Other Podcast 44",
                                    "Other Podcast 43",
                                    "Other Podcast 42",
                                    "Other Podcast 41",
                                    "Other Podcast 40",
                                    "Other Podcast 39"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for second page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(1, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf(), listOf(NOT_DOWNLOADED), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(1)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Other Podcast 38",
                                    "Other Podcast 37",
                                    "Other Podcast 36",
                                    "Other Podcast 35",
                                    "Other Podcast 34",
                                    "Other Podcast 33",
                                    "Other Podcast 32",
                                    "Other Podcast 31",
                                    "Other Podcast 30",
                                    "Other Podcast 29",
                                    "Other Podcast 28",
                                    "Other Podcast 27"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for before last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(3, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf(), listOf(NOT_DOWNLOADED), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(3)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).contains(
                                    "Other Podcast 14",
                                    "Other Podcast 13",
                                    "Other Podcast 12",
                                    "Other Podcast 11",
                                    "Other Podcast 10",
                                    "Other Podcast 9",
                                    "Other Podcast 8",
                                    "Other Podcast 7",
                                    "Other Podcast 6",
                                    "Other Podcast 5",
                                    "Other Podcast 4",
                                    "Other Podcast 3"
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `for last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(4, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf(), listOf(NOT_DOWNLOADED), page, null))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(2)
                            assertThat(it.first).isFalse()
                            assertThat(it.last).isTrue()
                            assertThat(it.number).isEqualTo(4)
                            assertThat(it.numberOfElements).isEqualTo(2)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title))
                                    .contains("Other Podcast 2", "Other Podcast 1")
                        }
                        .verifyComplete()
            }
        }

        @Nested
        @DisplayName("with podcast id")
        inner class WithPodcastId {

            private val podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8")

            @Test
            fun `67b56578-454b-40a5-8d55-5fe1a14673e8`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                StepVerifier.create(repository.search("", listOf(), listOf(), page, podcastId))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.content.size).isEqualTo(12)
                            assertThat(it.first).isTrue()
                            assertThat(it.last).isFalse()
                            assertThat(it.number).isEqualTo(0)
                            assertThat(it.numberOfElements).isEqualTo(12)
                            assertThat(it.totalElements).isEqualTo(50)
                            assertThat(it.totalPages).isEqualTo(5)
                            assertThat(it.content.map(Item::title)).containsExactly(
                                    "Appload 50",
                                    "Appload 49",
                                    "Appload 48",
                                    "Appload 47",
                                    "Appload 46",
                                    "Appload 45",
                                    "Appload 44",
                                    "Appload 43",
                                    "Appload 42",
                                    "Appload 41",
                                    "Appload 40",
                                    "Appload 39"
                            )
                        }
                        .verifyComplete()
            }
        }

    }

    @Nested
    @DisplayName("should create")
    inner class ShouldCreate {

        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Test
        fun `a simple item`() {
            /* Given */
            val item = ItemForCreation(
                    title = "an item",
                    url = "http://foo.bar.com/an_item",

                    pubDate = now(),
                    downloadDate = now(),
                    creationDate = now(),

                    description = "a description",
                    mimeType = "audio/mp3",
                    length = 1234,
                    fileName = "ofejeaoijefa.mp3",
                    status = FINISH,

                    podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                    cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/item.jpg"))
            )
            val numberOfItem = query.selectCount().from(ITEM).fetchOne(count())
            val numberOfCover = query.selectCount().from(COVER).fetchOne(count())

            /* When */
            StepVerifier.create(repository.create(item))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.title).isEqualTo("an item")
                        assertThat(it.url).isEqualTo("http://foo.bar.com/an_item")
                        assertThat(it.pubDate).isCloseTo(now(), within(10, SECONDS))
                        assertThat(it.downloadDate).isCloseTo(now(), within(10, SECONDS))
                        assertThat(it.creationDate).isCloseTo(now(), within(10, SECONDS))
                        assertThat(it.description).isEqualTo("a description")
                        assertThat(it.mimeType).isEqualTo("audio/mp3")
                        assertThat(it.length).isEqualTo(1234)
                        assertThat(it.fileName).isEqualTo("ofejeaoijefa.mp3")
                        assertThat(it.status).isEqualTo(FINISH)

                        assertThat(it.podcast.id).isEqualTo(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"))
                        assertThat(it.podcast.title).isEqualTo("Geek Inc HD")
                        assertThat(it.podcast.url).isEqualTo("http://fake.url.com/rss")

                        assertThat(it.cover.height).isEqualTo(100)
                        assertThat(it.cover.width).isEqualTo(100)
                        assertThat(it.cover.url).isEqualTo("http://foo.bar.com/cover/item.jpg")
                    }
                    .verifyComplete()

            assertThat(numberOfItem + 1).isEqualTo(query.selectCount().from(ITEM).fetchOne(count()))
            assertThat(numberOfCover + 1).isEqualTo(query.selectCount().from(COVER).fetchOne(count()))
        }

        @Test
        fun `but found an already existing item so don't do anything and return empty`() {
            /* Given */
            val item = ItemForCreation(
                    title = "an item",
                    url = "http://fakeurl.com/geekinc.123.mp3",

                    pubDate = now(),
                    downloadDate = now(),
                    creationDate = now(),

                    description = "a description",
                    mimeType = "audio/mp3",
                    length = 1234,
                    fileName = "ofejeaoijefa.mp3",
                    status = FINISH,

                    podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                    cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/item.jpg"))
            )
            val numberOfItem = query.selectCount().from(ITEM).fetchOne(count())
            val numberOfCover = query.selectCount().from(COVER).fetchOne(count())

            /* When */
            StepVerifier.create(repository.create(item))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            assertThat(numberOfItem).isEqualTo(query.selectCount().from(ITEM).fetchOne(count()))
            assertThat(numberOfCover).isEqualTo(query.selectCount().from(COVER).fetchOne(count()))
        }
    }


}
