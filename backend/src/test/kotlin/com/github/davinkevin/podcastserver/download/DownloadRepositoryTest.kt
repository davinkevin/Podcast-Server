package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.JooqR2DBCTest
import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.database.enums.DownloadingState
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.r2dbc
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import reactor.test.StepVerifier
import java.net.URI
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import kotlin.io.path.Path

private val fixedDate = Clock.fixed(OffsetDateTime.of(2022, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"))

/**
 * Created by kevin on 27/06/2020
 */
@JooqR2DBCTest
@Import(DownloadRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadRepositoryTest(
    @Autowired private val repo: DownloadRepository,
    @Autowired private val query: DSLContext
) {

    private val p = PODCAST
    private val c = COVER
    private val i = ITEM

    @BeforeAll
    fun beforeAll() {
        query.batch(
            truncate(DOWNLOADING_ITEM).cascade(),
            truncate(ITEM).cascade(),
            truncate(PODCAST).cascade(),
            truncate(COVER).cascade(),
        )
            .r2dbc()
            .execute()
    }

    @Nested
    @DisplayName("should init queue")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ShouldInitQueue {

        @Test
        fun `but found no item because item list is empty`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.initQueue(OffsetDateTime.now(fixedDate), 5))
                /* Then */
                .expectSubscription()
                .verifyComplete()

            val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
            assertThat(items).hasSize(0)
        }

        @Nested
        @DisplayName("with items")
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class WithItems {

            private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
            private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
            private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")

            private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
            private val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
            private val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
            private val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

            private val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            private val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            private val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

            private val now = OffsetDateTime.now(fixedDate)
            private val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
            private val twoDaysAgo = OffsetDateTime.now(fixedDate).minusDays(2)
            private val threeDaysAgo = OffsetDateTime.now(fixedDate).minusDays(3)

            @BeforeAll
            fun beforeAll() {
                query.batch(
                    truncate(ITEM).cascade(),
                    truncate(PODCAST).cascade(),
                    truncate(COVER).cascade(),
                    truncate(DOWNLOADING_ITEM).cascade(),

                    insertInto(COVER)
                        .columns(COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                        .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                        .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                    insertInto(p)
                        .columns(p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                        .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),
                )
                    .r2dbc()
                    .execute()
            }

            @BeforeEach
            fun beforeEach() {
                query.batch(
                    selectOne(),
                    truncate(DOWNLOADING_ITEM).cascade(),
                )
                    .r2dbc()
                    .execute()
            }

            @Nested
            @DisplayName("and filter on date")
            inner class AndFilterOnDate {

                @BeforeEach
                fun beforeEach() {
                    query.batch(
                        truncate(ITEM).cascade(),

                        insertInto(i)
                            .columns(i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                            .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                            .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                            .values(itemId3, twoDaysAgo, twoDaysAgo, twoDaysAgo, "desc item 3", Path(""), 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                            .values(itemId4, threeDaysAgo, threeDaysAgo, threeDaysAgo, "desc item 4", Path(""), 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),
                    )
                        .r2dbc()
                        .execute()
                }

                @Test
                fun `set to yesterday`() {
                    /* Given */
                    /* When */
                    StepVerifier.create(repo.initQueue(oneDayAgo, 999))
                        /* Then */
                        .expectSubscription()
                        .verifyComplete()

                    val (first) = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                    first.apply {
                        assertThat(itemId).isEqualTo(itemId1)
                        assertThat(position).isEqualTo(1)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                }

                @Test
                fun `set to two days ago`() {
                    /* Given */
                    /* When */
                    StepVerifier.create(repo.initQueue(twoDaysAgo, 999))
                        /* Then */
                        .expectSubscription()
                        .verifyComplete()

                    val (first, second) = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                    first.apply {
                        assertThat(itemId).isEqualTo(itemId2)
                        assertThat(position).isEqualTo(1)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                    second.apply {
                        assertThat(itemId).isEqualTo(itemId1)
                        assertThat(position).isEqualTo(2)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                }

                @Test
                fun `set to three days ago`() {
                    /* Given */
                    /* When */
                    StepVerifier.create(repo.initQueue(threeDaysAgo, 999))
                        /* Then */
                        .expectSubscription()
                        .verifyComplete()

                    val (first, second, third) = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                    first.apply {
                        assertThat(itemId).isEqualTo(itemId3)
                        assertThat(position).isEqualTo(1)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                    second.apply {
                        assertThat(itemId).isEqualTo(itemId2)
                        assertThat(position).isEqualTo(2)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                    third.apply {
                        assertThat(itemId).isEqualTo(itemId1)
                        assertThat(position).isEqualTo(3)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                }
            }

            @Nested
            @DisplayName("and filter on retry")
            inner class AndFilterOnRetry {

                @BeforeEach
                fun beforeEach() {
                    query.batch(
                        truncate(ITEM).cascade(),

                        insertInto(i)
                            .columns(i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                            .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                            .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                            .values(itemId3, twoDaysAgo, twoDaysAgo, twoDaysAgo, "desc item 3", Path(""), 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                            .values(itemId4, threeDaysAgo, threeDaysAgo, threeDaysAgo, "desc item 4", Path(""), 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),
                    )
                        .r2dbc()
                        .execute()
                }

                @Test
                fun `up to 0 max`() {
                    /* Given */
                    /* When */
                    StepVerifier.create(repo.initQueue(now.minusYears(1) , 0))
                        /* Then */
                        .expectSubscription()
                        .verifyComplete()

                    val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                    assertThat(items).isEmpty()
                }

                @Test
                fun `up to 5 max`() {
                    /* Given */
                    /* When */
                    StepVerifier.create(repo.initQueue(now.minusYears(1) , 5+1))
                        /* Then */
                        .expectSubscription()
                        .verifyComplete()

                    val (first) = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                    first.apply {
                        assertThat(itemId).isEqualTo(itemId1)
                        assertThat(position).isEqualTo(1)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                }

                @Test
                fun `up to 10 max`() {
                    /* Given */
                    /* When */
                    StepVerifier.create(repo.initQueue(now.minusYears(1) , 10+1))
                        /* Then */
                        .expectSubscription()
                        .verifyComplete()

                    val (first, second) = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                    first.apply {
                        assertThat(itemId).isEqualTo(itemId2)
                        assertThat(position).isEqualTo(1)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                    second.apply {
                        assertThat(itemId).isEqualTo(itemId1)
                        assertThat(position).isEqualTo(2)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                }

                @Test
                fun `up to 20 max`() {
                    /* Given */
                    /* When */
                    StepVerifier.create(repo.initQueue(now.minusYears(1) , 20+1))
                        /* Then */
                        .expectSubscription()
                        .verifyComplete()

                    val (first, second, third) = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                    first.apply {
                        assertThat(itemId).isEqualTo(itemId3)
                        assertThat(position).isEqualTo(1)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                    second.apply {
                        assertThat(itemId).isEqualTo(itemId2)
                        assertThat(position).isEqualTo(2)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                    third.apply {
                        assertThat(itemId).isEqualTo(itemId1)
                        assertThat(position).isEqualTo(3)
                        assertThat(state).isEqualTo(DownloadingState.WAITING)
                    }
                }
            }

        }

    }

    @Nested
    @DisplayName("should add item to queue")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ShouldAddItemToQueue {

        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")

        private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
        private val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
        private val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
        private val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

        private val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
        private val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
        private val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

        private val now = OffsetDateTime.now(fixedDate)
        private val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
        private val twoDaysAgo = OffsetDateTime.now(fixedDate).minusDays(2)
        private val threeDaysAgo = OffsetDateTime.now(fixedDate).minusDays(3)

        @BeforeAll
        fun beforeAll() {
            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),

                insertInto(COVER)
                    .columns(COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                insertInto(p)
                    .columns(p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),

                insertInto(i)
                    .columns(i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDaysAgo, twoDaysAgo, twoDaysAgo, "desc item 3", Path(""), 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDaysAgo, threeDaysAgo, threeDaysAgo, "desc item 4", Path(""), 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
            )
                .r2dbc()
                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                selectOne(),
                truncate(DOWNLOADING_ITEM).cascade(),
            )
                .r2dbc()
                .execute()
        }

        @Nested
        @DisplayName("with success")
        inner class WithSuccess {

            @Test
            fun `and a downloading list empty`() {
                /* Given */
                /* When */
                StepVerifier.create(repo.addItemToQueue(itemId1))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

                val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                assertThat(items).hasSize(1)
                items.last().apply {
                    assertThat(itemId).isEqualTo(itemId1)
                    assertThat(position).isEqualTo(1)
                    assertThat(state).isEqualTo(DownloadingState.WAITING)
                }
            }

            @Test
            fun `and downloading list with already one element`() {
                /* Given */
                query.batch(
                    selectOne(),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                        .values(itemId2, DownloadingState.WAITING, 1)
                )
                    .r2dbc().execute()

                /* When */
                StepVerifier.create(repo.addItemToQueue(itemId1))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

                val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                assertThat(items).hasSize(2)
                items.last().apply {
                    assertThat(itemId).isEqualTo(itemId1)
                    assertThat(position).isEqualTo(2)
                    assertThat(state).isEqualTo(DownloadingState.WAITING)
                }
            }

            @Test
            fun `and downloading list with already one element with a specific position`() {
                /* Given */
                query.batch(
                    selectOne(),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                        .values(itemId2, DownloadingState.WAITING, 123)
                )
                    .r2dbc().execute()

                /* When */
                StepVerifier.create(repo.addItemToQueue(itemId1))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

                val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                assertThat(items).hasSize(2)
                items.last().apply {
                    assertThat(itemId).isEqualTo(itemId1)
                    assertThat(position).isEqualTo(124)
                    assertThat(state).isEqualTo(DownloadingState.WAITING)
                }
            }

            @Test
            fun `and downloading list with multiple elements with spare positions`() {
                /* Given */
                query.batch(
                    selectOne(),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                        .values(itemId2, DownloadingState.WAITING, 128)
                        .values(itemId3, DownloadingState.WAITING, 256)
                )
                    .r2dbc().execute()

                /* When */
                StepVerifier.create(repo.addItemToQueue(itemId1))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

                val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                assertThat(items).hasSize(3)
                items.last().apply {
                    assertThat(itemId).isEqualTo(itemId1)
                    assertThat(position).isEqualTo(257)
                    assertThat(state).isEqualTo(DownloadingState.WAITING)
                }
            }

            @Test
            fun `and downloading list with item already present`() {
                /* Given */
                query.batch(
                    selectOne(),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                        .values(itemId2, DownloadingState.WAITING, 128)
                        .values(itemId3, DownloadingState.WAITING, 256)
                )
                    .r2dbc().execute()

                /* When */
                StepVerifier.create(repo.addItemToQueue(itemId3))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

                val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                assertThat(items).hasSize(2)
                items.last().apply {
                    assertThat(itemId).isEqualTo(itemId3)
                    assertThat(position).isEqualTo(256)
                    assertThat(state).isEqualTo(DownloadingState.WAITING)
                }
            }

            @Test
            fun `and downloading list with item already present with another state`() {
                /* Given */
                query.batch(
                    selectOne(),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                        .values(itemId2, DownloadingState.DOWNLOADING, 128)
                        .values(itemId3, DownloadingState.WAITING, 256)
                )
                    .r2dbc().execute()

                /* When */
                StepVerifier.create(repo.addItemToQueue(itemId2))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

                val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                assertThat(items).hasSize(2)
                items.first().apply {
                    assertThat(itemId).isEqualTo(itemId2)
                    assertThat(position).isEqualTo(128)
                    assertThat(state).isEqualTo(DownloadingState.DOWNLOADING)
                }
            }

            @Test
            fun `and do nothing because no item is defined for this id`() {
                /* Given */
                query.batch(
                    selectOne(),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                        .values(itemId2, DownloadingState.DOWNLOADING, 128)
                        .values(itemId3, DownloadingState.WAITING, 256)
                )
                    .r2dbc().execute()
                /* When */
                StepVerifier.create(repo.addItemToQueue(UUID.fromString("65a10b6e-5474-4e1c-9697-eba5330aee1d")))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

                val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                assertThat(items).hasSize(2)
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("should find all to download")
    inner class ShouldFindAllToDownload {

        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")

        private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
        private val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
        private val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
        private val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

        @BeforeAll
        fun beforeAll() {
            val now = OffsetDateTime.now(fixedDate)
            val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
            val twoDayAgo = OffsetDateTime.now(fixedDate).minusDays(2)
            val threeDayAgo = OffsetDateTime.now(fixedDate).minusDays(3)

            val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")
            val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),

                insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),

                insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)

            )
                .r2dbc()
                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                truncate(DOWNLOADING_ITEM).cascade()
            )
                .r2dbc()
                .execute()
        }

        @Test
        fun `but found no item because download list is empty`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.findAllToDownload(5))
                /* Then */
                .expectSubscription()
                .expectNextCount(0)
                .verifyComplete()
        }

        @Nested
        @DisplayName("with no item currently downloading")
        inner class WithNoItemCurrentlyDownloading {

            @Test
            fun `and return all item because the limit is greater than the number of available item`() {
                /* Given */
                query.batch(
                    select(value(1)),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION)
                        .values(itemId1, 1)
                        .values(itemId2, 2)
                        .values(itemId3, 3)
                        .values(itemId4, 4)
                )
                    .r2dbc()
                    .execute()
                /* When */
                StepVerifier.create(repo.findAllToDownload(30))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it.id).isEqualTo(itemId1) }
                    .assertNext { assertThat(it.id).isEqualTo(itemId2) }
                    .assertNext { assertThat(it.id).isEqualTo(itemId3) }
                    .assertNext { assertThat(it.id).isEqualTo(itemId4) }
                    .verifyComplete()
            }

            @Test
            fun `and return only the first`() {
                /* Given */
                query.batch(
                    select(value(1)),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION)
                        .values(itemId1, 1)
                        .values(itemId2, 2)
                        .values(itemId3, 3)
                        .values(itemId4, 4)
                )
                    .r2dbc()
                    .execute()
                /* When */
                StepVerifier.create(repo.findAllToDownload(1))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it.id).isEqualTo(itemId1) }
                    .verifyComplete()
            }

            @Test
            fun `and return only two`() {
                /* Given */
                query.batch(
                    select(value(1)),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION)
                        .values(itemId1, 1)
                        .values(itemId2, 2)
                        .values(itemId3, 3)
                        .values(itemId4, 4)
                )
                    .r2dbc()
                    .execute()
                /* When */
                StepVerifier.create(repo.findAllToDownload(2))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it.id).isEqualTo(itemId1) }
                    .assertNext { assertThat(it.id).isEqualTo(itemId2) }
                    .verifyComplete()
            }
        }

        @Nested
        @DisplayName("with item(s) already downloading")
        inner class WithItemAlreadyDownloading {

            @Test
            fun `1 downloading and 3 waiting`() {
                /* Given */
                query.batch(
                    select(value(1)),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.STATE)
                        .values(itemId1, 1, DownloadingState.DOWNLOADING)
                        .values(itemId2, 2, DownloadingState.WAITING)
                        .values(itemId3, 3, DownloadingState.WAITING)
                        .values(itemId4, 4, DownloadingState.WAITING)
                )
                    .r2dbc()
                    .execute()
                /* When */
                StepVerifier.create(repo.findAllToDownload(3))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it.id).isEqualTo(itemId2) }
                    .assertNext { assertThat(it.id).isEqualTo(itemId3) }
                    .verifyComplete()
            }

            @Test
            fun `2 downloading and 2 waiting`() {
                /* Given */
                query.batch(
                    select(value(1)),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.STATE)
                        .values(itemId1, 1, DownloadingState.DOWNLOADING)
                        .values(itemId2, 2, DownloadingState.DOWNLOADING)
                        .values(itemId3, 3, DownloadingState.WAITING)
                        .values(itemId4, 4, DownloadingState.WAITING)
                )
                    .r2dbc()
                    .execute()
                /* When */
                StepVerifier.create(repo.findAllToDownload(3))
                    /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it.id).isEqualTo(itemId3) }
                    .verifyComplete()
            }

            @Test
            fun `3 downloading and 1 waiting`() {
                /* Given */
                query.batch(
                    select(value(1)),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.STATE)
                        .values(itemId1, 1, DownloadingState.DOWNLOADING)
                        .values(itemId2, 2, DownloadingState.DOWNLOADING)
                        .values(itemId3, 3, DownloadingState.DOWNLOADING)
                        .values(itemId4, 4, DownloadingState.WAITING)
                )
                    .r2dbc()
                    .execute()
                /* When */
                StepVerifier.create(repo.findAllToDownload(3))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(0)
                    .verifyComplete()
            }

            @Test
            fun `4 downloading and 0 waiting`() {
                /* Given */
                query.batch(
                    select(value(1)),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.STATE)
                        .values(itemId1, 1, DownloadingState.DOWNLOADING)
                        .values(itemId2, 2, DownloadingState.DOWNLOADING)
                        .values(itemId3, 3, DownloadingState.DOWNLOADING)
                        .values(itemId4, 4, DownloadingState.DOWNLOADING)
                )
                    .r2dbc()
                    .execute()
                /* When */
                StepVerifier.create(repo.findAllToDownload(3))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(0)
                    .verifyComplete()
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("should find downloading")
    inner class ShouldFindAllDownloading {

        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")

        private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
        private val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
        private val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
        private val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

        private val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
        private val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
        private val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

        private val now = OffsetDateTime.now(fixedDate)
        private val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
        private val twoDaysAgo = OffsetDateTime.now(fixedDate).minusDays(2)
        private val threeDaysAgo = OffsetDateTime.now(fixedDate).minusDays(3)

        @BeforeAll
        fun beforeAll() {
            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),

                insertInto(COVER)
                    .columns(COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                insertInto(p)
                    .columns(p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),

                insertInto(i)
                    .columns(i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDaysAgo, twoDaysAgo, twoDaysAgo, "desc item 3", Path(""), 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDaysAgo, threeDaysAgo, threeDaysAgo, "desc item 4", Path(""), 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
            )
                .r2dbc()
                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                selectOne(),
                truncate(DOWNLOADING_ITEM).cascade(),
            )
                .r2dbc()
                .execute()
        }

        @Test
        fun `with no items`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.findAllDownloading())
                /* Then */
                .expectSubscription()
                .verifyComplete()
        }

        @Test
        fun `with 1 item not downloading`() {
            /* Given */
            query.batch(
                selectOne(),
                insertInto(DOWNLOADING_ITEM)
                    .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                    .values(itemId2, DownloadingState.WAITING, 123)
            )
                .r2dbc().execute()
            /* When */
            StepVerifier.create(repo.findAllDownloading())
                /* Then */
                .expectSubscription()
                .verifyComplete()
        }

        @Test
        fun `with 1 item downloading`() {
            /* Given */
            query.batch(
                selectOne(),
                insertInto(DOWNLOADING_ITEM)
                    .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                    .values(itemId2, DownloadingState.DOWNLOADING, 123)
            )
                .r2dbc().execute()

            /* When */
            StepVerifier.create(repo.findAllDownloading())
                /* Then */
                .expectSubscription()
                .expectNext(DownloadingItem(
                    id = itemId2,
                    title = "item_2",
                    status = Status.NOT_DOWNLOADED,
                    url = URI.create("https://foo.bar.com/item/2"),
                    numberOfFail = 10,
                    progression = 0,

                    podcast = DownloadingItem.Podcast(
                        id = podcastId,
                        title = "Podcast-Title"
                    ),

                    cover = DownloadingItem.Cover(
                        id = itemCoverId2,
                        url = URI.create("https://foo.bac.com/item/cover.jpg")
                    )
                ))
                .verifyComplete()
        }

        @Test
        fun `with 2 item downloading`() {
            /* Given */
            query.batch(
                selectOne(),
                insertInto(DOWNLOADING_ITEM)
                    .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                    .values(itemId2, DownloadingState.DOWNLOADING, 128)
                    .values(itemId3, DownloadingState.DOWNLOADING, 256)
            )
                .r2dbc().execute()

            /* When */
            StepVerifier.create(repo.findAllDownloading())
                /* Then */
                .expectSubscription()
                .expectNext(DownloadingItem(
                    id = itemId2,
                    title = "item_2",
                    status = Status.NOT_DOWNLOADED,
                    url = URI.create("https://foo.bar.com/item/2"),
                    numberOfFail = 10,
                    progression = 0,

                    podcast = DownloadingItem.Podcast(
                        id = podcastId,
                        title = "Podcast-Title"
                    ),

                    cover = DownloadingItem.Cover(
                        id = itemCoverId2,
                        url = URI.create("https://foo.bac.com/item/cover.jpg")
                    )
                ))
                .expectNext(DownloadingItem(
                    id = itemId3,
                    title = "item_3",
                    status = Status.NOT_DOWNLOADED,
                    url = URI.create("https://foo.bar.com/item/3"),
                    numberOfFail = 20,
                    progression = 0,

                    podcast = DownloadingItem.Podcast(
                        id = podcastId,
                        title = "Podcast-Title"
                    ),

                    cover = DownloadingItem.Cover(
                        id = itemCoverId3,
                        url = URI.create("https://foo.bac.com/item/cover.jpg")
                    )
                ))
                .verifyComplete()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("should find waiting")
    inner class ShouldFindAllWaiting {

        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")

        private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
        private val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
        private val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
        private val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

        private val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
        private val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
        private val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

        private val now = OffsetDateTime.now(fixedDate)
        private val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
        private val twoDaysAgo = OffsetDateTime.now(fixedDate).minusDays(2)
        private val threeDaysAgo = OffsetDateTime.now(fixedDate).minusDays(3)

        @BeforeAll
        fun beforeAll() {
            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),

                insertInto(COVER)
                    .columns(COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                insertInto(p)
                    .columns(p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),

                insertInto(i)
                    .columns(i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDaysAgo, twoDaysAgo, twoDaysAgo, "desc item 3", Path(""), 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDaysAgo, threeDaysAgo, threeDaysAgo, "desc item 4", Path(""), 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
            )
                .r2dbc()
                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                selectOne(),
                truncate(DOWNLOADING_ITEM).cascade(),
            )
                .r2dbc()
                .execute()
        }

        @Test
        fun `with no items`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.findAllWaiting())
                /* Then */
                .expectSubscription()
                .verifyComplete()
        }

        @Test
        fun `with 1 item not waiting`() {
            /* Given */
            query.batch(
                selectOne(),
                insertInto(DOWNLOADING_ITEM)
                    .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                    .values(itemId2, DownloadingState.DOWNLOADING, 123)
            )
                .r2dbc().execute()
            /* When */
            StepVerifier.create(repo.findAllWaiting())
                /* Then */
                .expectSubscription()
                .verifyComplete()
        }

        @Test
        fun `with 1 item waiting`() {
            /* Given */
            query.batch(
                selectOne(),
                insertInto(DOWNLOADING_ITEM)
                    .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                    .values(itemId2, DownloadingState.WAITING, 123)
            )
                .r2dbc().execute()

            /* When */
            StepVerifier.create(repo.findAllWaiting())
                /* Then */
                .expectSubscription()
                .expectNext(DownloadingItem(
                    id = itemId2,
                    title = "item_2",
                    status = Status.NOT_DOWNLOADED,
                    url = URI.create("https://foo.bar.com/item/2"),
                    numberOfFail = 10,
                    progression = 0,

                    podcast = DownloadingItem.Podcast(
                        id = podcastId,
                        title = "Podcast-Title"
                    ),

                    cover = DownloadingItem.Cover(
                        id = itemCoverId2,
                        url = URI.create("https://foo.bac.com/item/cover.jpg")
                    )
                ))
                .verifyComplete()
        }

        @Test
        fun `with 2 item waiting`() {
            /* Given */
            query.batch(
                selectOne(),
                insertInto(DOWNLOADING_ITEM)
                    .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.STATE, DOWNLOADING_ITEM.POSITION)
                    .values(itemId2, DownloadingState.WAITING, 128)
                    .values(itemId3, DownloadingState.WAITING, 256)
            )
                .r2dbc().execute()

            /* When */
            StepVerifier.create(repo.findAllWaiting())
                /* Then */
                .expectSubscription()
                .expectNext(DownloadingItem(
                    id = itemId2,
                    title = "item_2",
                    status = Status.NOT_DOWNLOADED,
                    url = URI.create("https://foo.bar.com/item/2"),
                    numberOfFail = 10,
                    progression = 0,

                    podcast = DownloadingItem.Podcast(
                        id = podcastId,
                        title = "Podcast-Title"
                    ),

                    cover = DownloadingItem.Cover(
                        id = itemCoverId2,
                        url = URI.create("https://foo.bac.com/item/cover.jpg")
                    )
                ))
                .expectNext(DownloadingItem(
                    id = itemId3,
                    title = "item_3",
                    status = Status.NOT_DOWNLOADED,
                    url = URI.create("https://foo.bar.com/item/3"),
                    numberOfFail = 20,
                    progression = 0,

                    podcast = DownloadingItem.Podcast(
                        id = podcastId,
                        title = "Podcast-Title"
                    ),

                    cover = DownloadingItem.Cover(
                        id = itemCoverId3,
                        url = URI.create("https://foo.bac.com/item/cover.jpg")
                    )
                ))
                .verifyComplete()
        }

    }


    @Nested
    @DisplayName("should stop item")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ShouldStopItem {

        private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")

        @BeforeAll
        fun beforeAll() {
            val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

            val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
            val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
            val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

            val now = OffsetDateTime.now(fixedDate)
            val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
            val twoDayAgo = OffsetDateTime.now(fixedDate).minusDays(2)
            val threeDayAgo = OffsetDateTime.now(fixedDate).minusDays(3)

            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),

                insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg"),

                insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),

                insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),
            )
                .r2dbc()
                .execute()
        }

        @Test
        fun `with success`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.stopItem(itemId1))
                /* Then */
                .expectSubscription()
                .expectNext(1)
                .verifyComplete()

            val numberOfStoppedItems = query.selectCount().from(ITEM).where(ITEM.STATUS.eq(Status.STOPPED))
                .r2dbc().fetchOne(count())

            assertThat(numberOfStoppedItems).isEqualTo(1)
        }



        @Test
        fun `and let others in same state as before`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.stopItem(itemId1))
                /* Then */
                .expectSubscription()
                .expectNext(1)
                .verifyComplete()

            val notStoppedItems = query
                .selectFrom(ITEM)
                .where(ITEM.STATUS.notEqual(Status.STOPPED))
                .r2dbc()
                .fetch()

            assertThat(notStoppedItems).hasSize(3)
            assertThat(notStoppedItems.map { it[ITEM.STATUS] }).containsOnly(Status.NOT_DOWNLOADED)
        }

        @AfterAll
        fun afterAll() {
            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),
            )
                .r2dbc()
                .execute()
        }
    }

    @Nested
    @DisplayName("should update download item")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ShouldUpdateDownloadItem {

        private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")

        @BeforeAll
        fun beforeAll() {
            val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

            val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
            val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
            val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

            val now = OffsetDateTime.now(fixedDate)
            val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
            val twoDayAgo = OffsetDateTime.now(fixedDate).minusDays(2)
            val threeDayAgo = OffsetDateTime.now(fixedDate).minusDays(3)


            query.batch(
                insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg"),

                insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),

                insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),
            )
                .r2dbc()
                .execute()

        }

        private val downloadingItem = DownloadingItem(
            id = itemId1,
            title = "",
            status = Status.STOPPED,
            url = URI("https://foo.com/item/1"),
            numberOfFail = 6,
            progression = 59,
            podcast = DownloadingItem.Podcast(podcastId, "podcast title"),
            cover = DownloadingItem.Cover(coverId, URI("https://foo.com/item/cover.png"))
        )

        @ParameterizedTest
        @EnumSource(Status::class)
        fun `with status`(status: Status) {
            /* Given */
            val withStatus = downloadingItem.copy(status = status)
            /* When */
            StepVerifier.create(repo.updateDownloadItem(withStatus))
                /* Then */
                .expectSubscription()
                .expectNext(1)
                .verifyComplete()

            val item = query.selectFrom(i).where(i.ID.eq(downloadingItem.id)).r2dbc().fetchOne() ?: error("item not found")
            assertThat(item[ITEM.STATUS]).isEqualTo(status)
            val others = query.selectFrom(i).where(i.ID.notEqual(downloadingItem.id)).r2dbc().fetch()
            assertThat(others.map { it[i.STATUS] }).containsOnly(Status.NOT_DOWNLOADED)
        }

        @ParameterizedTest(name = "with fails x{0}")
        @ValueSource( ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
        fun `with fail`(numberOfFails: Int) {
            /* Given */
            val withFails = downloadingItem.copy(numberOfFail = numberOfFails)
            /* When */
            StepVerifier.create(repo.updateDownloadItem(withFails))
                /* Then */
                .expectSubscription()
                .expectNext(1)
                .verifyComplete()

            val item = query.selectFrom(i).where(i.ID.eq(downloadingItem.id)).r2dbc().fetchOne() ?: error("item not found")
            assertThat(item[i.NUMBER_OF_FAIL]).isEqualTo(numberOfFails)
            val others = query.selectFrom(i).where(i.ID.notEqual(downloadingItem.id)).r2dbc().fetch()
            assertThat(others.map { it[i.NUMBER_OF_FAIL] }).containsOnly(6)
        }

        @AfterAll
        fun afterAll() {
            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),
            )
                .r2dbc()
                .execute()
        }
    }

    @Nested
    @DisplayName("should finish download")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ShouldFinishDownload {

        private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")

        @BeforeAll
        fun beforeAll() {
            val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

            val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
            val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
            val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

            val now = OffsetDateTime.now(fixedDate)
            val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
            val twoDayAgo = OffsetDateTime.now(fixedDate).minusDays(2)
            val threeDayAgo = OffsetDateTime.now(fixedDate).minusDays(3)

            query.batch(
                insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg"),

                insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),

                insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),
            )
                .r2dbc()
                .execute()
        }

        @Test
        fun `with success`() {
            /* Given */
            val now = OffsetDateTime.now(fixedDate)
            /* When */
            StepVerifier.create(repo.finishDownload(
                id = itemId1,
                length = 100L,
                mimeType = "video/avi",
                fileName = Path("filename.mp4"),
                downloadDate = now
            ))
                /* Then */
                .expectSubscription()
                .expectNext(1)
                .verifyComplete()

            val item = query.selectFrom(i).where(i.ID.eq(itemId1)).r2dbc().fetchOne() ?: error("item not found")
            assertThat(item[i.STATUS]).isEqualTo(Status.FINISH)
            assertThat(item[i.LENGTH]).isEqualTo(100L)
            assertThat(item[i.MIME_TYPE]).isEqualTo("video/avi")
            assertThat(item[i.FILE_NAME]).isEqualTo(Path("filename.mp4"))
            assertThat(item[i.DOWNLOAD_DATE]).isEqualToIgnoringNanos(now)
        }

        @Test
        fun `without changing other elements`() {
            /* Given */
            val now = OffsetDateTime.now(fixedDate)
            val itemsBefore = query.selectFrom(i).where(i.ID.notEqual(itemId1)).r2dbc().fetch()
            /* When */
            StepVerifier.create(repo.finishDownload(
                id = itemId1,
                length = 100L,
                mimeType = "video/avi",
                fileName = Path("filename.mp4"),
                downloadDate = now
            ))
                /* Then */
                .expectSubscription()
                .expectNext(1)
                .verifyComplete()

            val itemsAfter = query.selectFrom(i).where(i.ID.notEqual(itemId1)).r2dbc().fetch()
            assertThat(itemsBefore).containsAll(itemsAfter)
        }

        @AfterAll
        fun afterAll() {
            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),
            )
                .r2dbc()
                .execute()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("should remove from queue")
    inner class ShouldRemoveFromQueue {

        private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
        private val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")

        @BeforeEach
        fun beforeEach() {
            val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

            val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
            val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

            val now = OffsetDateTime.now(fixedDate)
            val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
            val twoDayAgo = OffsetDateTime.now(fixedDate).minusDays(2)
            val threeDayAgo = OffsetDateTime.now(fixedDate).minusDays(3)

            query.batch(

                truncate(COVER).cascade(),
                truncate(PODCAST).cascade(),
                truncate(DOWNLOADING_ITEM).cascade(),
                truncate(ITEM).cascade(),

                insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg"),

                insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),

                insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),

                insertInto(DOWNLOADING_ITEM)
                    .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION)
                    .values(itemId1, 1)
                    .values(itemId2, 2)
            )
                .r2dbc()
                .execute()
        }

        @Test
        fun `with success`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.remove(id = itemId1, hasToBeStopped = false))
                /* Then */
                .expectSubscription()
                .verifyComplete()

            val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                .map { it[DOWNLOADING_ITEM.ITEM_ID] to it[DOWNLOADING_ITEM.POSITION] }
            assertThat(items).contains(itemId2 to 2)
        }

        @Test
        fun `with stop action on the item`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.remove(id = itemId1, hasToBeStopped = true))
                /* Then */
                .expectSubscription()
                .verifyComplete()

            val items = query.selectFrom(DOWNLOADING_ITEM).r2dbc().fetch()
                .map { it[DOWNLOADING_ITEM.ITEM_ID] to it[DOWNLOADING_ITEM.POSITION] }
            assertThat(items).contains(itemId2 to 2)
            val status = query.selectFrom(i).where(i.ID.eq(itemId1)).r2dbc().fetchOne(i.STATUS)
            assertThat(status).isEqualTo(Status.STOPPED)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("should move into queue")
    inner class ShouldMoveIntoQueue {

        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")

        private val itemId1 = UUID.fromString("13106eba-b89b-11ea-b3de-0242ac130004")
        private val itemId2 = UUID.fromString("2ec458db-9ac8-4553-a5d0-ce5559f6e225")
        private val itemId3 = UUID.fromString("3a52242b-9002-4615-b1cd-4fe1cb55f079")
        private val itemId4 = UUID.fromString("4efb65e3-25cc-4944-8234-98082df21e84")

        @BeforeAll
        fun beforeAll() {
            val now = OffsetDateTime.now(fixedDate)
            val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
            val twoDayAgo = OffsetDateTime.now(fixedDate).minusDays(2)
            val threeDayAgo = OffsetDateTime.now(fixedDate).minusDays(3)

            val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")
            val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),

                insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),

                insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)

            )
                .r2dbc()
                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                truncate(DOWNLOADING_ITEM).cascade()
            )
                .r2dbc()
                .execute()
        }

        @Nested
        @DisplayName("with items in downloading items table")
        inner class WithItemsInDownloadingItemsTable {

            @Test
            fun `move item up to first`() {
                /* Given */
                query.batch(
                    select(value(1)),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.STATE)
                        .values(itemId1, 1, DownloadingState.DOWNLOADING)
                        .values(itemId2, 2, DownloadingState.WAITING)
                        .values(itemId3, 3, DownloadingState.WAITING)
                        .values(itemId4, 4, DownloadingState.WAITING)
                )
                    .r2dbc()
                    .execute()
                /* When */
                StepVerifier.create(repo.moveItemInQueue(itemId3, 0))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()


                val items = query.selectFrom(DOWNLOADING_ITEM).orderBy(DOWNLOADING_ITEM.POSITION).r2dbc().fetch()
                assertThat(items[0].itemId).isEqualTo(itemId1)
                assertThat(items[1].itemId).isEqualTo(itemId3)
                assertThat(items[2].itemId).isEqualTo(itemId2)
                assertThat(items[3].itemId).isEqualTo(itemId4)
            }

            @Test
            fun `move item down to second`() {
                /* Given */
                query.batch(
                    select(value(1)),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.STATE)
                        .values(itemId1, 1, DownloadingState.DOWNLOADING)
                        .values(itemId2, 2, DownloadingState.WAITING)
                        .values(itemId3, 3, DownloadingState.WAITING)
                        .values(itemId4, 4, DownloadingState.WAITING)
                )
                    .r2dbc()
                    .execute()
                /* When */
                StepVerifier.create(repo.moveItemInQueue(itemId2, 1))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

                val items = query.selectFrom(DOWNLOADING_ITEM).orderBy(DOWNLOADING_ITEM.POSITION).r2dbc().fetch()
                assertThat(items[0].itemId).isEqualTo(itemId1)
                assertThat(items[1].itemId).isEqualTo(itemId3)
                assertThat(items[2].itemId).isEqualTo(itemId2)
                assertThat(items[3].itemId).isEqualTo(itemId4)
            }

            @Test
            fun `move item up to last`() {
                /* Given */
                query.batch(
                    select(value(1)),
                    insertInto(DOWNLOADING_ITEM)
                        .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.STATE)
                        .values(itemId1, 1, DownloadingState.DOWNLOADING)
                        .values(itemId2, 2, DownloadingState.WAITING)
                        .values(itemId3, 3, DownloadingState.WAITING)
                        .values(itemId4, 4, DownloadingState.WAITING)
                )
                    .r2dbc()
                    .execute()
                /* When */
                StepVerifier.create(repo.moveItemInQueue(itemId2, 2))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

                val items = query.selectFrom(DOWNLOADING_ITEM).orderBy(DOWNLOADING_ITEM.POSITION).r2dbc().fetch()
                assertThat(items[0].itemId).isEqualTo(itemId1)
                assertThat(items[1].itemId).isEqualTo(itemId3)
                assertThat(items[2].itemId).isEqualTo(itemId4)
                assertThat(items[3].itemId).isEqualTo(itemId2)
            }


        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("should start item")
    inner class ShouldStartItem {

        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")

        private val itemId1 = UUID.fromString("13106eba-b89b-11ea-b3de-0242ac130004")
        private val itemId2 = UUID.fromString("2ec458db-9ac8-4553-a5d0-ce5559f6e225")
        private val itemId3 = UUID.fromString("3a52242b-9002-4615-b1cd-4fe1cb55f079")
        private val itemId4 = UUID.fromString("4efb65e3-25cc-4944-8234-98082df21e84")

        @BeforeAll
        fun beforeAll() {
            val now = OffsetDateTime.now(fixedDate)
            val oneDayAgo = OffsetDateTime.now(fixedDate).minusDays(1)
            val twoDayAgo = OffsetDateTime.now(fixedDate).minusDays(2)
            val threeDayAgo = OffsetDateTime.now(fixedDate).minusDays(3)

            val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")
            val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")

            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),

                insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg"),

                insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(fixedDate), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId),

                insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.GUID, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)

            )
                .r2dbc()
                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                truncate(DOWNLOADING_ITEM).cascade(),
                insertInto(DOWNLOADING_ITEM)
                    .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.STATE)
                    .values(itemId1, 1, DownloadingState.DOWNLOADING)
                    .values(itemId2, 2, DownloadingState.WAITING)
                    .values(itemId3, 3, DownloadingState.WAITING)
                    .values(itemId4, 4, DownloadingState.WAITING)
            )
                .r2dbc()
                .execute()
        }

        @Test
        fun `with success`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.startItem(itemId2))
            /* Then */
                .expectSubscription()
                .verifyComplete()

            val item = query.selectFrom(DOWNLOADING_ITEM).orderBy(DOWNLOADING_ITEM.POSITION)
                .r2dbc().fetch()
                .first { it.itemId == itemId2 }
            assertThat(item.state).isEqualTo(DownloadingState.DOWNLOADING)
            assertThat(item.position).isEqualTo(2)
        }

        @Test
        fun `with one item not in the list`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.startItem(UUID.fromString("1811fadd-45e6-4761-8ad0-6a72951cb255")))
            /* Then */
                .expectSubscription()
                .verifyComplete()

            val (first, second, third, fourth) = query.selectFrom(DOWNLOADING_ITEM).orderBy(DOWNLOADING_ITEM.POSITION)
                .r2dbc().fetch()

            assertThat(first.itemId).isEqualTo(itemId1)
            assertThat(first.position).isEqualTo(1)
            assertThat(first.state).isEqualTo(DownloadingState.DOWNLOADING)

            assertThat(second.itemId).isEqualTo(itemId2)
            assertThat(second.position).isEqualTo(2)
            assertThat(second.state).isEqualTo(DownloadingState.WAITING)

            assertThat(third.itemId).isEqualTo(itemId3)
            assertThat(third.position).isEqualTo(3)
            assertThat(third.state).isEqualTo(DownloadingState.WAITING)

            assertThat(fourth.itemId).isEqualTo(itemId4)
            assertThat(fourth.position).isEqualTo(4)
            assertThat(fourth.state).isEqualTo(DownloadingState.WAITING)
        }

    }
}
