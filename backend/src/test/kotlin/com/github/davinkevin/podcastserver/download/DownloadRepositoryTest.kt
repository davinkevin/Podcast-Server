package com.github.davinkevin.podcastserver.download

import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import reactor.test.StepVerifier
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

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
        query.truncate(PODCAST).cascade().execute()
        query.truncate(COVER).cascade().execute()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("should find all to download")
    inner class ShouldFindAllToDownload {

        private val coverId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")
        private val podcastId = UUID.fromString("32ec6d44-b880-11ea-b3de-0242ac130004")

        @BeforeAll
        fun beforeAll() {
            query.insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .execute()

            val p = PODCAST
            query.insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId)
                    .execute()
        }

        @Test
        fun `but found no item because database is empty`() {
            /* Given */
            val from = OffsetDateTime.now().minusYears(5)
            val maxRetry = 5
            /* When */
            StepVerifier.create(repo.findAllToDownload(from, maxRetry))
                    /* Then */
                    .expectSubscription()
                    .expectNextCount(0)
                    .verifyComplete()
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("with items filtered by date")
        inner class WithItemsFilteredByDate {

            private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")
            private val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            private val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            private val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")
            private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
            private val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
            private val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
            private val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

            @BeforeAll
            fun beforeAll() {
                query.insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                        .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .execute()

                val now = OffsetDateTime.now()
                val lastWeek = now.minusWeeks(1)
                val twentyNineDays = now.minusDays(29)
                val thirtyOneDays = now.minusDays(31)
                val fiftyDays = now.minusDays(50)
                query.insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.COVER_ID, i.PODCAST_ID)
                        .values(itemId1, lastWeek, lastWeek, lastWeek, "desc item 1", "", 123, "foo/bar", 0, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                        .values(itemId2, twentyNineDays, twentyNineDays, twentyNineDays, "desc item 2", "", 1, "video/mp4", 1, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                        .values(itemId3, thirtyOneDays, thirtyOneDays, thirtyOneDays, "desc item 3", "", 1, "video/mp4", 2, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                        .values(itemId4, fiftyDays, fiftyDays, fiftyDays, "desc item 4", "", 1, "video/mp4", 3, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
                        .execute()
            }

            @Test
            fun `in 5 days`() {
                /* Given */
                val fromDate = OffsetDateTime.now().minusDays(5)
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 1000))
                        /* Then */
                        .expectSubscription()
                        .verifyComplete()
            }

            @Test
            fun `in 20 days`() {
                /* Given */
                val fromDate = OffsetDateTime.now().minusDays(20)
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 1000))
                        /* Then */
                        .expectSubscription()
                        .assertNext(::assertFirst)
                        .verifyComplete()
            }

            @Test
            fun `in 30 days`() {
                /* Given */
                val fromDate = OffsetDateTime.now().minusDays(30)
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 1000))
                        /* Then */
                        .expectSubscription()
                        .assertNext(::assertSecond)
                        .assertNext(::assertFirst)
                        .verifyComplete()
            }

            @Test
            fun `in 50 days`() {
                /* Given */
                val fromDate = OffsetDateTime.now().minusDays(50)
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 1000))
                        /* Then */
                        .expectSubscription()
                        .assertNext(::assertThird)
                        .assertNext(::assertSecond)
                        .assertNext(::assertFirst)
                        .verifyComplete()
            }

            @Test
            fun `in 100 days`() {
                /* Given */
                val fromDate = OffsetDateTime.now().minusDays(100)
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 1000))
                        /* Then */
                        .expectSubscription()
                        .assertNext(::assertFourth)
                        .assertNext(::assertThird)
                        .assertNext(::assertSecond)
                        .assertNext(::assertFirst)
                        .verifyComplete()
            }

            private fun assertFirst(it: DownloadingItem) {
                assertThat(it.id).isEqualTo(itemId1)
                assertThat(it.title).isEqualTo("item_1")
                assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                assertThat(it.url).isEqualTo(URI("https://foo.bar.com/item/1"))
                assertThat(it.numberOfFail).isEqualTo(0)
                assertThat(it.progression).isEqualTo(0)
                assertThat(it.podcast.id).isEqualTo(podcastId)
                assertThat(it.podcast.title).isEqualTo("Podcast-Title")
                assertThat(it.cover.id).isEqualTo(itemCoverId1)
                assertThat(it.cover.url).isEqualTo(URI("https://foo.bac.com/item/cover.jpg"))
            }

            private fun assertSecond(it: DownloadingItem) {
                assertThat(it.id).isEqualTo(itemId2)
                assertThat(it.title).isEqualTo("item_2")
                assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                assertThat(it.url).isEqualTo(URI("https://foo.bar.com/item/2"))
                assertThat(it.numberOfFail).isEqualTo(1)
                assertThat(it.progression).isEqualTo(0)
                assertThat(it.podcast.id).isEqualTo(podcastId)
                assertThat(it.podcast.title).isEqualTo("Podcast-Title")
                assertThat(it.cover.id).isEqualTo(itemCoverId2)
                assertThat(it.cover.url).isEqualTo(URI("https://foo.bac.com/item/cover.jpg"))
            }

            private fun assertThird(it: DownloadingItem) {
                assertThat(it.id).isEqualTo(itemId3)
                assertThat(it.title).isEqualTo("item_3")
                assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                assertThat(it.url).isEqualTo(URI("https://foo.bar.com/item/3"))
                assertThat(it.numberOfFail).isEqualTo(2)
                assertThat(it.progression).isEqualTo(0)
                assertThat(it.podcast.id).isEqualTo(podcastId)
                assertThat(it.podcast.title).isEqualTo("Podcast-Title")
                assertThat(it.cover.id).isEqualTo(itemCoverId3)
                assertThat(it.cover.url).isEqualTo(URI("https://foo.bac.com/item/cover.jpg"))
            }

            private fun assertFourth(it: DownloadingItem) {
                assertThat(it.id).isEqualTo(itemId4)
                assertThat(it.title).isEqualTo("item_4")
                assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                assertThat(it.url).isEqualTo(URI("https://foo.bar.com/item/4"))
                assertThat(it.numberOfFail).isEqualTo(3)
                assertThat(it.progression).isEqualTo(0)
                assertThat(it.podcast.id).isEqualTo(podcastId)
                assertThat(it.podcast.title).isEqualTo("Podcast-Title")
                assertThat(it.cover.id).isEqualTo(itemCoverId4)
                assertThat(it.cover.url).isEqualTo(URI("https://foo.bac.com/item/cover.jpg"))
            }

            @AfterAll
            fun afterAll() {
                query.truncate(ITEM).cascade().execute()
                query.deleteFrom(COVER)
                        .where(COVER.ID.`in`(itemCoverId1, itemCoverId2, itemCoverId3, itemCoverId4))
                        .execute()
            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("with items filtered status")
        inner class WithItemsFilteredByStatus {

            private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")
            private val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            private val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            private val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")
            private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
            private val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
            private val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
            private val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

            @BeforeAll
            fun beforeAll() {
                query.insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                        .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .execute()

                val now = OffsetDateTime.now()
                query.insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.COVER_ID, i.PODCAST_ID)
                        .values(itemId1, now, now, now, "desc item 1", "", 123, "foo/bar", 0, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                        .values(itemId2, now, now, now, "desc item 2", "", 1, "video/mp4", 1, Status.STOPPED, "item_2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                        .values(itemId3, now, now, now, "desc item 3", "", 1, "video/mp4", 2, Status.STARTED, "item_3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                        .values(itemId4, now, now, now, "desc item 4", "", 1, "video/mp4", 3, Status.FINISH, "item_4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
                        .values(UUID.randomUUID(), now, now, now, "desc item 4", "", 1, "video/mp4", 3, Status.DELETED, "item_4", "https://foo.bar.com/item/5", itemCoverId4, podcastId)
                        .values(UUID.randomUUID(), now, now, now, "desc item 4", "", 1, "video/mp4", 3, Status.FAILED, "item_4", "https://foo.bar.com/item/6", itemCoverId4, podcastId)
                        .values(UUID.randomUUID(), now, now, now, "desc item 4", "", 1, "video/mp4", 3, Status.PAUSED, "item_4", "https://foo.bar.com/item/7", itemCoverId4, podcastId)
                        .execute()
            }

            @Test
            fun `not downloaded`() {
                /* Given */
                val fromDate = OffsetDateTime.now().minusDays(1000)
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 1000))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.id).isEqualTo(itemId1)
                            assertThat(it.title).isEqualTo("item_1")
                            assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                            assertThat(it.url).isEqualTo(URI("https://foo.bar.com/item/1"))
                            assertThat(it.numberOfFail).isEqualTo(0)
                            assertThat(it.progression).isEqualTo(0)
                            assertThat(it.podcast.id).isEqualTo(podcastId)
                            assertThat(it.podcast.title).isEqualTo("Podcast-Title")
                            assertThat(it.cover.id).isEqualTo(itemCoverId1)
                            assertThat(it.cover.url).isEqualTo(URI("https://foo.bac.com/item/cover.jpg"))
                        }
                        .verifyComplete()
            }

            @AfterAll
            fun afterAll() {
                query.truncate(ITEM).cascade().execute()
                query.deleteFrom(COVER)
                        .where(COVER.ID.`in`(itemCoverId1, itemCoverId2, itemCoverId3, itemCoverId4))
                        .execute()
            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        @DisplayName("with items filtered by number of fails")
        inner class WithItemsFilteredByNumberOfFails {

            private val itemCoverId1 = UUID.fromString("e0010c64-b89a-11ea-b3de-0242ac130004")
            private val itemCoverId2 = UUID.fromString("5a75e2de-5393-4f5e-9707-e5e806ada10f")
            private val itemCoverId3 = UUID.fromString("4e9e654c-7c30-4ff5-826b-a39eb1f57e79")
            private val itemCoverId4 = UUID.fromString("bd567872-55da-4801-8d3e-4bf2e79c2b65")
            private val itemId1 = UUID.fromString("63106eba-b89b-11ea-b3de-0242ac130004")
            private val itemId2 = UUID.fromString("9ec458db-9ac8-4553-a5d0-ce5559f6e225")
            private val itemId3 = UUID.fromString("7a52242b-9002-4615-b1cd-4fe1cb55f079")
            private val itemId4 = UUID.fromString("2efb65e3-25cc-4944-8234-98082df21e84")

            private val fromDate = OffsetDateTime.now().minusDays(1000)

            @BeforeAll
            fun beforeAll() {
                val c = COVER
                query.insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                        .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg")
                        .execute()

                val i = ITEM
                val now = OffsetDateTime.now()
                val oneDayAgo = OffsetDateTime.now().minusDays(1)
                val twoDayAgo = OffsetDateTime.now().minusDays(2)
                val threeDayAgo = OffsetDateTime.now().minusDays(3)
                query.insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.COVER_ID, i.PODCAST_ID)
                        .values(itemId1, now, now, now, "desc item 1", "", 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                        .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", "", 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                        .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", "", 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                        .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", "", 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
                        .execute()
            }

            @Test
            fun `less than 5 retries`() {
                /* Given */
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 5))
                        /* Then */
                        .expectSubscription()
                        .verifyComplete()
            }

            @Test
            fun `less than 10 retries`() {
                /* Given */
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 10))
                        /* Then */
                        .expectSubscription()
                        .assertNext(::assertFirst)
                        .verifyComplete()
            }

            @Test
            fun `less than 20 retries`() {
                /* Given */
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 20))
                        /* Then */
                        .expectSubscription()
                        .assertNext(::assertSecond)
                        .assertNext(::assertFirst)
                        .verifyComplete()
            }

            @Test
            fun `less than 30 retries`() {
                /* Given */
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 30))
                        /* Then */
                        .expectSubscription()
                        .assertNext(::assertThird)
                        .assertNext(::assertSecond)
                        .assertNext(::assertFirst)
                        .verifyComplete()
            }

            @Test
            fun `less than 45 retries`() {
                /* Given */
                /* When */
                StepVerifier.create(repo.findAllToDownload(fromDate, 45))
                        /* Then */
                        .expectSubscription()
                        .assertNext(::assertFourth)
                        .assertNext(::assertThird)
                        .assertNext(::assertSecond)
                        .assertNext(::assertFirst)
                        .verifyComplete()
            }

            private fun assertFirst(it: DownloadingItem) {
                assertThat(it.id).isEqualTo(itemId1)
                assertThat(it.title).isEqualTo("item_1")
                assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                assertThat(it.url).isEqualTo(URI("https://foo.bar.com/item/1"))
                assertThat(it.numberOfFail).isEqualTo(5)
                assertThat(it.progression).isEqualTo(0)
                assertThat(it.podcast.id).isEqualTo(podcastId)
                assertThat(it.podcast.title).isEqualTo("Podcast-Title")
                assertThat(it.cover.id).isEqualTo(itemCoverId1)
                assertThat(it.cover.url).isEqualTo(URI("https://foo.bac.com/item/cover.jpg"))
            }

            private fun assertSecond(it: DownloadingItem) {
                assertThat(it.id).isEqualTo(itemId2)
                assertThat(it.title).isEqualTo("item_2")
                assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                assertThat(it.url).isEqualTo(URI("https://foo.bar.com/item/2"))
                assertThat(it.numberOfFail).isEqualTo(10)
                assertThat(it.progression).isEqualTo(0)
                assertThat(it.podcast.id).isEqualTo(podcastId)
                assertThat(it.podcast.title).isEqualTo("Podcast-Title")
                assertThat(it.cover.id).isEqualTo(itemCoverId2)
                assertThat(it.cover.url).isEqualTo(URI("https://foo.bac.com/item/cover.jpg"))
            }

            private fun assertThird(it: DownloadingItem) {
                assertThat(it.id).isEqualTo(itemId3)
                assertThat(it.title).isEqualTo("item_3")
                assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                assertThat(it.url).isEqualTo(URI("https://foo.bar.com/item/3"))
                assertThat(it.numberOfFail).isEqualTo(20)
                assertThat(it.progression).isEqualTo(0)
                assertThat(it.podcast.id).isEqualTo(podcastId)
                assertThat(it.podcast.title).isEqualTo("Podcast-Title")
                assertThat(it.cover.id).isEqualTo(itemCoverId3)
                assertThat(it.cover.url).isEqualTo(URI("https://foo.bac.com/item/cover.jpg"))
            }

            private fun assertFourth(it: DownloadingItem) {
                assertThat(it.id).isEqualTo(itemId4)
                assertThat(it.title).isEqualTo("item_4")
                assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                assertThat(it.url).isEqualTo(URI("https://foo.bar.com/item/4"))
                assertThat(it.numberOfFail).isEqualTo(30)
                assertThat(it.progression).isEqualTo(0)
                assertThat(it.podcast.id).isEqualTo(podcastId)
                assertThat(it.podcast.title).isEqualTo("Podcast-Title")
                assertThat(it.cover.id).isEqualTo(itemCoverId4)
                assertThat(it.cover.url).isEqualTo(URI("https://foo.bac.com/item/cover.jpg"))
            }

            @AfterAll
            fun afterAll() {
                query.truncate(ITEM).cascade().execute()
                query.deleteFrom(COVER)
                        .where(COVER.ID.`in`(itemCoverId1, itemCoverId2, itemCoverId3, itemCoverId4))
                        .execute()
            }
        }

        @AfterAll
        fun afterAll() {
            query.truncate(PODCAST).cascade().execute()
            query.truncate(COVER).cascade().execute()
        }
    }

    @Nested
    @DisplayName("should find download item by id")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ShouldFindDownloadItemById {

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

            query.insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .execute()


            query.insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId)
                    .execute()

            query.insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .execute()

            val now = OffsetDateTime.now()
            val oneDayAgo = OffsetDateTime.now().minusDays(1)
            val twoDayAgo = OffsetDateTime.now().minusDays(2)
            val threeDayAgo = OffsetDateTime.now().minusDays(3)
            query.insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", "", 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", "", 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", "", 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", "", 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
                    .execute()
        }

        @Test
        fun `with success`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.findDownloadingItemById(itemId1))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isEqualTo(itemId1)
                        assertThat(it.title).isEqualTo("item_1")
                        assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                        assertThat(it.url).isEqualTo(URI("https://foo.bar.com/item/1"))
                        assertThat(it.numberOfFail).isEqualTo(5)
                        assertThat(it.progression).isEqualTo(0)
                        assertThat(it.podcast.id).isEqualTo(podcastId)
                        assertThat(it.podcast.title).isEqualTo("Podcast-Title")
                        assertThat(it.cover.id).isEqualTo(itemCoverId1)
                        assertThat(it.cover.url).isEqualTo(URI("https://foo.bac.com/item/cover.jpg"))
                    }
                    .verifyComplete()
        }

        @Test
        fun `without any matching results`() {
            /* Given */
            /* When */
            StepVerifier.create(repo.findDownloadingItemById(UUID.fromString("09314033-7939-4f40-99bf-d01acce0f912")))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @AfterAll
        fun afterAll() {
            query.truncate(ITEM).cascade().execute()
            query.truncate(PODCAST).cascade().execute()
            query.truncate(COVER).cascade().execute()
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

            query.insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .execute()

            query.insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId)
                    .execute()

            query.insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .execute()

            val now = OffsetDateTime.now()
            val oneDayAgo = OffsetDateTime.now().minusDays(1)
            val twoDayAgo = OffsetDateTime.now().minusDays(2)
            val threeDayAgo = OffsetDateTime.now().minusDays(3)
            query.insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", "", 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", "", 1, "video/mp4", 10, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", "", 1, "video/mp4", 20, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", "", 1, "video/mp4", 30, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
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

            val numberOfStoppedItems = query.selectCount().from(ITEM).where(ITEM.STATUS.eq(Status.STOPPED)).fetchOne(DSL.count())
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
                    .fetch()

            assertThat(notStoppedItems).hasSize(3)
            assertThat(notStoppedItems.map { it[ITEM.STATUS] }).containsOnly(Status.NOT_DOWNLOADED)
        }

        @AfterAll
        fun afterAll() {
            query.truncate(ITEM).cascade().execute()
            query.truncate(PODCAST).cascade().execute()
            query.truncate(COVER).cascade().execute()
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

            query.insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .execute()

            query.insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId)
                    .execute()

            query.insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .execute()

            val now = OffsetDateTime.now()
            val oneDayAgo = OffsetDateTime.now().minusDays(1)
            val twoDayAgo = OffsetDateTime.now().minusDays(2)
            val threeDayAgo = OffsetDateTime.now().minusDays(3)
            query.insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", "", 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", "", 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", "", 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", "", 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
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

            val item = query.selectFrom(i).where(i.ID.eq(downloadingItem.id)).fetchOne() ?: error("item not found")
            assertThat(item[ITEM.STATUS]).isEqualTo(status)
            val others = query.selectFrom(i).where(i.ID.notEqual(downloadingItem.id)).fetch()
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

            val item = query.selectFrom(i).where(i.ID.eq(downloadingItem.id)).fetchOne() ?: error("item not found")
            assertThat(item[i.NUMBER_OF_FAIL]).isEqualTo(numberOfFails)
            val others = query.selectFrom(i).where(i.ID.notEqual(downloadingItem.id)).fetch()
            assertThat(others.map { it[i.NUMBER_OF_FAIL] }).containsOnly(6)
        }

        @AfterAll
        fun afterAll() {
            query.truncate(ITEM).cascade().execute()
            query.truncate(PODCAST).cascade().execute()
            query.truncate(COVER).cascade().execute()
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

            query.insertInto(COVER, COVER.ID, COVER.HEIGHT, COVER.WIDTH, COVER.URL)
                    .values(coverId, 100, 100, "https://foo.bac.com/cover.jpg")
                    .execute()

            query.insertInto(p, p.ID, p.DESCRIPTION, p.HAS_TO_BE_DELETED, p.LAST_UPDATE, p.SIGNATURE, p.TITLE, p.TYPE, p.URL, p.COVER_ID)
                    .values(podcastId, "desc", true, OffsetDateTime.now(), "sign", "Podcast-Title", "Youtube", "https://www.youtube.com/channel/UCx83f-KzDd3o1QK2AdJIftg", coverId)
                    .execute()

            query.insertInto(c, c.ID, c.HEIGHT, c.WIDTH, c.URL)
                    .values(itemCoverId1, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId2, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId3, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .values(itemCoverId4, 100, 100, "https://foo.bac.com/item/cover.jpg")
                    .execute()

            val now = OffsetDateTime.now()
            val oneDayAgo = OffsetDateTime.now().minusDays(1)
            val twoDayAgo = OffsetDateTime.now().minusDays(2)
            val threeDayAgo = OffsetDateTime.now().minusDays(3)
            query.insertInto(i, i.ID, i.CREATION_DATE, i.PUB_DATE, i.DOWNLOAD_DATE, i.DESCRIPTION, i.FILE_NAME, i.LENGTH, i.MIME_TYPE, i.NUMBER_OF_FAIL, i.STATUS, i.TITLE, i.URL, i.COVER_ID, i.PODCAST_ID)
                    .values(itemId1, now, now, now, "desc item 1", "", 123, "foo/bar", 5, Status.NOT_DOWNLOADED, "item_1", "https://foo.bar.com/item/1", itemCoverId1, podcastId)
                    .values(itemId2, oneDayAgo, oneDayAgo, oneDayAgo, "desc item 2", "", 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_2", "https://foo.bar.com/item/2", itemCoverId2, podcastId)
                    .values(itemId3, twoDayAgo, twoDayAgo, twoDayAgo, "desc item 3", "", 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_3", "https://foo.bar.com/item/3", itemCoverId3, podcastId)
                    .values(itemId4, threeDayAgo, threeDayAgo, threeDayAgo, "desc item 4", "", 1, "video/mp4", 6, Status.NOT_DOWNLOADED, "item_4", "https://foo.bar.com/item/4", itemCoverId4, podcastId)
                    .execute()
        }

        @Test
        fun `with success`() {
            /* Given */
            val now = OffsetDateTime.now()
            /* When */
            StepVerifier.create(repo.finishDownload(
                    id = itemId1,
                    length = 100L,
                    mimeType = "video/avi",
                    fileName = "filename.mp4",
                    downloadDate = now
            ))
                    /* Then */
                    .expectSubscription()
                    .expectNext(1)
                    .verifyComplete()

            val item = query.selectFrom(i).where(i.ID.eq(itemId1)).fetchOne() ?: error("item not found")
            assertThat(item[i.STATUS]).isEqualTo(Status.FINISH)
            assertThat(item[i.LENGTH]).isEqualTo(100L)
            assertThat(item[i.MIME_TYPE]).isEqualTo("video/avi")
            assertThat(item[i.FILE_NAME]).isEqualTo("filename.mp4")
            assertThat(item[i.DOWNLOAD_DATE]).isEqualTo(now)
        }

        @Test
        fun `without changing other elements`() {
            /* Given */
            val now = OffsetDateTime.now()
            val itemsBefore = query.selectFrom(i).where(i.ID.notEqual(itemId1)).fetch()
            /* When */
            StepVerifier.create(repo.finishDownload(
                    id = itemId1,
                    length = 100L,
                    mimeType = "video/avi",
                    fileName = "filename.mp4",
                    downloadDate = now
            ))
                    /* Then */
                    .expectSubscription()
                    .expectNext(1)
                    .verifyComplete()

            val itemsAfter = query.selectFrom(i).where(i.ID.notEqual(itemId1)).fetch()
            assertThat(itemsBefore).containsAll(itemsAfter)
        }

        @AfterAll
        fun afterAll() {
            query.truncate(ITEM).cascade().execute()
            query.truncate(PODCAST).cascade().execute()
            query.truncate(COVER).cascade().execute()
        }
    }
}
