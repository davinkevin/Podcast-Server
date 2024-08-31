package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.database.enums.DownloadingState
import com.github.davinkevin.podcastserver.database.enums.ItemStatus
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.toDb
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.net.URI
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.io.path.Path

private val fixedDate = Clock.fixed(OffsetDateTime.of(2022, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"))

/**
 * Created by kevin on 27/06/2020
 */
@JooqTest
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
            repo.initQueue(OffsetDateTime.now(fixedDate), 5)

            /* Then */
            val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    .execute()
            }

            @BeforeEach
            fun beforeEach() {
                query.batch(
                    selectOne(),
                    truncate(DOWNLOADING_ITEM).cascade(),
                )
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
                            .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                            .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                            .values(itemId3, twoDaysAgo, twoDaysAgo, twoDaysAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                            .values(itemId4, threeDaysAgo, threeDaysAgo, threeDaysAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),
                    )
                        .execute()
                }

                @Test
                fun `set to yesterday`() {
                    /* Given */
                    /* When */
                    repo.initQueue(oneDayAgo, 999)
                    /* Then */

                    val (first) = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    repo.initQueue(twoDaysAgo, 999)

                    /* Then */
                    val (first, second) = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    repo.initQueue(threeDaysAgo, 999)

                    /* Then */
                    val (first, second, third) = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                            .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                            .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                            .values(itemId3, twoDaysAgo, twoDaysAgo, twoDaysAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                            .values(itemId4, threeDaysAgo, threeDaysAgo, threeDaysAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),
                    )

                        .execute()
                }

                @Test
                fun `up to 0 max`() {
                    /* Given */
                    /* When */
                    repo.initQueue(now.minusYears(1) , 0)

                    /* Then */
                    val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
                    assertThat(items).isEmpty()
                }

                @Test
                fun `up to 5 max`() {
                    /* Given */
                    /* When */
                    repo.initQueue(now.minusYears(1) , 5+1)

                    /* Then */
                    val (first) = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    repo.initQueue(now.minusYears(1) , 10+1)

                    /* Then */
                    val (first, second) = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    repo.initQueue(now.minusYears(1) , 20+1)

                    /* Then */
                    val (first, second, third) = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDaysAgo, twoDaysAgo, twoDaysAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDaysAgo, threeDaysAgo, threeDaysAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
            )

                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                selectOne(),
                truncate(DOWNLOADING_ITEM).cascade(),
            )

                .execute()
        }

        @Nested
        @DisplayName("with success")
        inner class WithSuccess {

            @Test
            fun `and a downloading list empty`() {
                /* Given */
                /* When */
                repo.addItemToQueue(itemId1)

                /* Then */
                val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    .execute()

                /* When */
                repo.addItemToQueue(itemId1)

                /* Then */
                val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    .execute()

                /* When */
                repo.addItemToQueue(itemId1)

                /* Then */
                val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    .execute()

                /* When */
                repo.addItemToQueue(itemId1)

                /* Then */
                val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    .execute()

                /* When */
                repo.addItemToQueue(itemId3)

                /* Then */
                val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    .execute()

                /* When */
                repo.addItemToQueue(itemId2)

                /* Then */
                val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    .execute()
                /* When */
                repo.addItemToQueue(UUID.fromString("65a10b6e-5474-4e1c-9697-eba5330aee1d"))

                /* Then */
                val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)

            )

                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                truncate(DOWNLOADING_ITEM).cascade()
            )

                .execute()
        }

        @Test
        fun `but found no item because download list is empty`() {
            /* Given */
            /* When */
            val items = repo.findAllToDownload(5)
            /* Then */
            assertThat(items).isEmpty()
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

                    .execute()
                /* When */
                val items = repo.findAllToDownload(30)

                /* Then */
                assertThat(items).hasSize(4)
                val (first, second, third, fourth) = items
                assertAll {
                    assertThat(first.id).isEqualTo(itemId1)
                    assertThat(second.id).isEqualTo(itemId2)
                    assertThat(third.id).isEqualTo(itemId3)
                    assertThat(fourth.id).isEqualTo(itemId4)
                }
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

                    .execute()
                /* When */
                val items = repo.findAllToDownload(1)

                /* Then */
                assertThat(items).hasSize(1)
                assertThat(items.first().id).isEqualTo(itemId1)
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

                    .execute()
                /* When */
                val items = repo.findAllToDownload(2)

                /* Then */

                assertThat(items).hasSize(2)
                val (first, second) = items
                assertThat(first.id).isEqualTo(itemId1)
                assertThat(second.id).isEqualTo(itemId2)
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

                    .execute()
                /* When */
                val items = repo.findAllToDownload(3)

                /* Then */
                assertThat(items).hasSize(2)
                val (first, second) = items
                assertThat(first.id).isEqualTo(itemId2)
                assertThat(second.id).isEqualTo(itemId3)
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

                    .execute()

                /* When */
                val items = repo.findAllToDownload(3)

                /* Then */
                assertThat(items).hasSize(1)
                assertThat(items.first().id).isEqualTo(itemId3)
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

                    .execute()
                /* When */
                val items = repo.findAllToDownload(3)

                /* Then */
                assertThat(items).isEmpty()
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

                    .execute()

                /* When */
                val items = repo.findAllToDownload(3)

                /* Then */
                assertThat(items).isEmpty()
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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDaysAgo, twoDaysAgo, twoDaysAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDaysAgo, threeDaysAgo, threeDaysAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
            )

                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                selectOne(),
                truncate(DOWNLOADING_ITEM).cascade(),
            )

                .execute()
        }

        @Test
        fun `with no items`() {
            /* Given */
            /* When */
            val items = repo.findAllDownloading()
            /* Then */
            assertThat(items).isEmpty()
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
                .execute()

            /* When */
            val items = repo.findAllDownloading()

            /* Then */
            assertThat(items).isEmpty()
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
                .execute()

            /* When */
            val items = repo.findAllDownloading()

            /* Then */
            assertThat(items).hasSize(1)
            assertThat(items.first()).isEqualTo(
                DownloadingItem(
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
            )
            )
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
                .execute()

            /* When */
            val items = repo.findAllDownloading()

            /* Then */
            assertThat(items).hasSize(2)
            val (first, second) = items
            assertAll {
                assertThat(first).isEqualTo(
                    DownloadingItem(
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
                )
                )
                assertThat(second).isEqualTo(
                    DownloadingItem(
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
                )
                )
            }
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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDaysAgo, twoDaysAgo, twoDaysAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDaysAgo, threeDaysAgo, threeDaysAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
            )

                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                selectOne(),
                truncate(DOWNLOADING_ITEM).cascade(),
            )

                .execute()
        }

        @Test
        fun `with no items`() {
            /* Given */
            /* When */
            val items = repo.findAllWaiting()

            /* Then */
            assertThat(items).isEmpty()
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
                .execute()

            /* When */
            val items = repo.findAllWaiting()

            /* Then */
            assertThat(items).isEmpty()
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
                .execute()

            /* When */
            val items = repo.findAllWaiting()

            /* Then */
            assertThat(items).hasSize(1)
            assertThat(items.first()).isEqualTo(
                DownloadingItem(
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
            )
            )
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
                .execute()

            /* When */
            val items = repo.findAllWaiting()

            /* Then */
            assertThat(items).hasSize(2)
            val (first, second) = items
            assertAll {
                assertThat(first).isEqualTo(
                    DownloadingItem(
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
                )
                )
                assertThat(second).isEqualTo(
                    DownloadingItem(
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
                )
                )
            }
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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),
            )

                .execute()
        }

        @Test
        fun `with success`() {
            /* Given */

            /* When */
            repo.stopItem(itemId1)

            /* Then */
            val numberOfStoppedItems = query.selectCount().from(ITEM).where(ITEM.STATUS.eq(ItemStatus.STOPPED))
                .fetchOne(count())

            assertThat(numberOfStoppedItems).isEqualTo(1)
        }



        @Test
        fun `and let others in same state as before`() {
            /* Given */

            /* When */
            repo.stopItem(itemId1)

            /* Then */
            val notStoppedItems = query
                .selectFrom(ITEM)
                .where(ITEM.STATUS.notEqual(ItemStatus.STOPPED))

                .fetch()

            assertThat(notStoppedItems).hasSize(3)
            assertThat(notStoppedItems.map { it[ITEM.STATUS] }).containsOnly(ItemStatus.NOT_DOWNLOADED)
        }

        @AfterAll
        fun afterAll() {
            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),
            )

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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 6, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 6, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 6, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),
            )

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
            repo.updateDownloadItem(withStatus)

            /* Then */
            val item = query.selectFrom(i).where(i.ID.eq(downloadingItem.id)).fetchOne() ?: error("item not found")
            assertThat(item[ITEM.STATUS]).isEqualTo(status.toDb())
            val others = query.selectFrom(i).where(i.ID.notEqual(downloadingItem.id)).fetch()
            assertThat(others.map { it[i.STATUS] }).containsOnly(ItemStatus.NOT_DOWNLOADED)
        }

        @ParameterizedTest(name = "with fails x{0}")
        @ValueSource( ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
        fun `with fail`(numberOfFails: Int) {
            /* Given */
            val withFails = downloadingItem.copy(numberOfFail = numberOfFails)

            /* When */
            repo.updateDownloadItem(withFails)

            /* Then */
            val item = query.selectFrom(i).where(i.ID.eq(downloadingItem.id)).fetchOne() ?: error("item not found")
            assertThat(item[i.NUMBER_OF_FAIL]).isEqualTo(numberOfFails)
            val others = query.selectFrom(i).where(i.ID.notEqual(downloadingItem.id)).fetch()
            assertThat(others.map { it[i.NUMBER_OF_FAIL] }).containsOnly(6)
        }

        @AfterAll
        fun afterAll() {
            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),
            )

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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 6, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 6, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 6, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),
            )

                .execute()
        }

        @Test
        fun `with success`() {
            /* Given */
            val now = OffsetDateTime.now(fixedDate)

            /* When */
            repo.finishDownload(
                id = itemId1,
                length = 100L,
                mimeType = "video/avi",
                fileName = Path("filename.mp4"),
                downloadDate = now
            )

            /* Then */
            val item = query.selectFrom(i).where(i.ID.eq(itemId1)).fetchOne() ?: error("item not found")
            assertThat(item[i.STATUS]).isEqualTo(ItemStatus.FINISH)
            assertThat(item[i.LENGTH]).isEqualTo(100L)
            assertThat(item[i.MIME_TYPE]).isEqualTo("video/avi")
            assertThat(item[i.FILE_NAME]).isEqualTo(Path("filename.mp4"))
            assertThat(item[i.DOWNLOAD_DATE]).isCloseTo(now, within(1, ChronoUnit.SECONDS))
        }

        @Test
        fun `without changing other elements`() {
            /* Given */
            val now = OffsetDateTime.now(fixedDate)
            val itemsBefore = query.selectFrom(i).where(i.ID.notEqual(itemId1)).fetch()

            /* When */
            repo.finishDownload(
                id = itemId1,
                length = 100L,
                mimeType = "video/avi",
                fileName = Path("filename.mp4"),
                downloadDate = now
            )

            /* Then */
            val itemsAfter = query.selectFrom(i).where(i.ID.notEqual(itemId1)).fetch()
            assertThat(itemsBefore).containsAll(itemsAfter)
        }

        @AfterAll
        fun afterAll() {
            query.batch(
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),
            )

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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId),

                insertInto(DOWNLOADING_ITEM)
                    .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION)
                    .values(itemId1, 1)
                    .values(itemId2, 2)
            )

                .execute()
        }

        @Test
        fun `with success`() {
            /* Given */

            /* When */
            repo.remove(id = itemId1, hasToBeStopped = false)

            /* Then */
            val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
                .map { it[DOWNLOADING_ITEM.ITEM_ID] to it[DOWNLOADING_ITEM.POSITION] }
            assertThat(items).contains(itemId2 to 2)
        }

        @Test
        fun `with stop action on the item`() {
            /* Given */
            /* When */
            repo.remove(id = itemId1, hasToBeStopped = true)

            /* Then */
            val items = query.selectFrom(DOWNLOADING_ITEM).fetch()
                .map { it[DOWNLOADING_ITEM.ITEM_ID] to it[DOWNLOADING_ITEM.POSITION] }
            assertThat(items).contains(itemId2 to 2)
            val status = query.selectFrom(i).where(i.ID.eq(itemId1)).fetchOne(i.STATUS)
            assertThat(status).isEqualTo(ItemStatus.STOPPED)
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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)

            )

                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                truncate(DOWNLOADING_ITEM).cascade()
            )

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

                    .execute()

                /* When */
                repo.moveItemInQueue(itemId3, 0)

                /* Then */
                val items = query.selectFrom(DOWNLOADING_ITEM).orderBy(DOWNLOADING_ITEM.POSITION).fetch()
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

                    .execute()

                /* When */
                repo.moveItemInQueue(itemId2, 1)

                /* Then */
                val items = query.selectFrom(DOWNLOADING_ITEM).orderBy(DOWNLOADING_ITEM.POSITION).fetch()
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

                    .execute()
                /* When */
                repo.moveItemInQueue(itemId2, 2)

                /* Then */
                val items = query.selectFrom(DOWNLOADING_ITEM).orderBy(DOWNLOADING_ITEM.POSITION).fetch()
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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)

            )

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

                .execute()
        }

        @Test
        fun `with success`() {
            /* Given */
            /* When */
            repo.startItem(itemId2)

            /* Then */
            val item = query.selectFrom(DOWNLOADING_ITEM).orderBy(DOWNLOADING_ITEM.POSITION)
                .fetch()
                .first { it.itemId == itemId2 }
            assertThat(item.state).isEqualTo(DownloadingState.DOWNLOADING)
            assertThat(item.position).isEqualTo(2)
        }

        @Test
        fun `with one item not in the list`() {
            /* Given */
            /* When */
            repo.startItem(UUID.fromString("1811fadd-45e6-4761-8ad0-6a72951cb255"))

            /* Then */
            val (first, second, third, fourth) = query.selectFrom(DOWNLOADING_ITEM).orderBy(DOWNLOADING_ITEM.POSITION)
                .fetch()

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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("should reset all item in downloading state")
    inner class ShouldResetAllItemInDownloadingState {

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
                    .values(itemId1, now, now, now, "desc item 1", Path(""), 123, "foo/bar", 5, ItemStatus.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", Path(""), 1, "video/mp4", 10, ItemStatus.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", Path(""), 1, "video/mp4", 20, ItemStatus.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", Path(""), 1, "video/mp4", 30, ItemStatus.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)

            )

                .execute()
        }

        @BeforeEach
        fun beforeEach() {
            query.batch(
                truncate(DOWNLOADING_ITEM).cascade(),
                insertInto(DOWNLOADING_ITEM)
                    .columns(DOWNLOADING_ITEM.ITEM_ID, DOWNLOADING_ITEM.POSITION, DOWNLOADING_ITEM.STATE)
                    .values(itemId1, 1, DownloadingState.WAITING)
                    .values(itemId2, 2, DownloadingState.WAITING)
                    .values(itemId3, 3, DownloadingState.WAITING)
                    .values(itemId4, 4, DownloadingState.WAITING)
            )

                .execute()
        }

        @Test
        fun `with 0 item in downloading state`() {
            /* Given */
            /* When */
            repo.resetToWaitingStateAllDownloadingItems()

            /* Then */
            val items = query
                .selectFrom(DOWNLOADING_ITEM)
                .fetch()

            assertThat(items.all { it.state == DownloadingState.WAITING }).isTrue()
        }

        @Test
        fun `with 1 item in downloading state`() {
            /* Given */
            query.batch(
                selectOne(),
                update(DOWNLOADING_ITEM)
                    .set(DOWNLOADING_ITEM.STATE, DownloadingState.DOWNLOADING)
                    .where(DOWNLOADING_ITEM.POSITION.`in`(1))
            )

                .execute()

            /* When */
            val result = repo.resetToWaitingStateAllDownloadingItems()

            /* Then */
            assertThat(result).isEqualTo(1)

            val items = query
                .selectFrom(DOWNLOADING_ITEM)
                .fetch()

            assertThat(items.all { it.state == DownloadingState.WAITING }).isTrue()
        }

        @Test
        fun `with 2 items in downloading state`() {
            /* Given */
            query.batch(
                selectOne(),
                update(DOWNLOADING_ITEM)
                    .set(DOWNLOADING_ITEM.STATE, DownloadingState.DOWNLOADING)
                    .where(DOWNLOADING_ITEM.POSITION.`in`(1, 2))
            )

                .execute()

            /* When */
            val result = repo.resetToWaitingStateAllDownloadingItems()

            /* Then */
            assertThat(result).isEqualTo(2)
            val items = query
                .selectFrom(DOWNLOADING_ITEM)
                .fetch()

            assertThat(items.all { it.state == DownloadingState.WAITING }).isTrue()
        }

        @Test
        fun `with 3 items in downloading state`() {
            /* Given */
            query.batch(
                selectOne(),
                update(DOWNLOADING_ITEM)
                    .set(DOWNLOADING_ITEM.STATE, DownloadingState.DOWNLOADING)
                    .where(DOWNLOADING_ITEM.POSITION.`in`(1, 2, 3))
            )

                .execute()

            /* When */
            val result = repo.resetToWaitingStateAllDownloadingItems()

            /* Then */
            assertThat(result).isEqualTo(3)
            val items = query
                .selectFrom(DOWNLOADING_ITEM)
                .fetch()

            assertThat(items.all { it.state == DownloadingState.WAITING }).isTrue()
        }

        @Test
        fun `with all items in downloading state`() {
            /* Given */
            query.batch(
                selectOne(),
                update(DOWNLOADING_ITEM)
                    .set(DOWNLOADING_ITEM.STATE, DownloadingState.DOWNLOADING)
            )

                .execute()

            /* When */
            val result = repo.resetToWaitingStateAllDownloadingItems()

            /* Then */
            assertThat(result).isEqualTo(4)
            val items = query
                .selectFrom(DOWNLOADING_ITEM)
                .fetch()

            assertThat(items.all { it.state == DownloadingState.WAITING }).isTrue()
        }

    }
}
