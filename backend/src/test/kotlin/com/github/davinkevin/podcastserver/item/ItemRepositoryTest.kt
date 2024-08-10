package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.cover.CoverForCreation
import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.database.enums.ItemStatus
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import org.assertj.core.api.Assertions.*
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import java.net.URI
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*
import java.util.UUID.fromString
import java.util.function.Consumer
import kotlin.io.path.Path

/**
 * Created by kevin on 2019-02-09
 */
@JooqTest
@Import(ItemRepository::class)
class ItemRepositoryTest(
    @Autowired val query: DSLContext,
    @Autowired val repository: ItemRepository
) {

    private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

    @BeforeEach
    fun beforeEach() {
        query.batch(
            truncate(PLAYLIST_ITEMS).cascade(),
            truncate(PODCAST_TAGS).cascade(),
            truncate(PLAYLIST).cascade(),
            truncate(PODCAST).cascade(),
            truncate(COVER).cascade(),
            truncate(TAG).cascade(),
        )
            .execute()
    }

    @Nested
    @DisplayName("Should find")
    inner class ShouldFindById {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                insertInto(COVER)
                    .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                    .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                    .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100),

                insertInto(PODCAST)
                    .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "http://fakeurl.com/appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "http://fakeurl.com/appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate.minusDays(30), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "http://fakeurl.com/appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate, null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "http://fakeurl.com/geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, fixedDate.minusYears(1), fixedDate, fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "http://fakeurl.com/geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "http://fakeurl.com/geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(2), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "http://fakeurl.com/geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(1), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                insertInto(TAG)
                    .columns(TAG.ID, TAG.NAME)
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                insertInto(PODCAST_TAGS)
                    .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")),

                insertInto(PLAYLIST)
                    .columns(PLAYLIST.ID, PLAYLIST.NAME, PLAYLIST.COVER_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist", fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind", fromString("51a51b22-4a6f-45df-8905-f5e691eafee4")),

                insertInto(PLAYLIST_ITEMS)
                    .columns(PLAYLIST_ITEMS.PLAYLISTS_ID, PLAYLIST_ITEMS.ITEMS_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
            )

                .execute()
        }

        @Test
        fun `by id and return one matching element`() {
            /* Given */
            val id = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")

            /* When */
            val item = repository.findById(id)!!

            /* Then */
            assertThat(item.id).isEqualTo(id)
        }

        @Test
        fun `by id and return empty if not find by id`() {
            /* Given */
            val id = fromString("98b33370-a976-4e4d-9ab8-57d47241e693")

            /* When */
            val item = repository.findById(id)

            /* Then */
            assertThat(item).isNull()
        }
    }

    @Nested
    @DisplayName("Should find all")
    inner class ShouldFindAll {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                insertInto(COVER)
                    .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                    .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                    .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100),

                insertInto(PODCAST)
                    .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "http://fakeurl.com/appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "http://fakeurl.com/appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate.minusDays(30), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "http://fakeurl.com/appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate, null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "http://fakeurl.com/geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, fixedDate.minusYears(1), fixedDate, fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "http://fakeurl.com/geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "http://fakeurl.com/geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(2), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "http://fakeurl.com/geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(1), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                insertInto(TAG)
                    .columns(TAG.ID, TAG.NAME)
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                insertInto(PODCAST_TAGS)
                    .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")),

                insertInto(PLAYLIST)
                    .columns(PLAYLIST.ID, PLAYLIST.NAME, PLAYLIST.COVER_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist", fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind", fromString("51a51b22-4a6f-45df-8905-f5e691eafee4")),

                insertInto(PLAYLIST_ITEMS)
                    .columns(PLAYLIST_ITEMS.PLAYLISTS_ID, PLAYLIST_ITEMS.ITEMS_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
            )

                .execute()
        }


        @Test
        fun `to delete`() {
            /* Given */
            /* When */
            val it = repository.findAllToDelete(fixedDate)
            /* Then */
            assertThat(it)
                .hasSize(1)
                .containsOnly(DeleteItemRequest(
                    id = fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"),
                    fileName = Path("geekinc.124.mp3"),
                    podcastTitle = "Geek Inc HD"
                ))
        }
    }

    @Nested
    @DisplayName("should delete")
    inner class ShouldDelete {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                insertInto(COVER)
                    .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                    .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                    .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100),

                insertInto(PODCAST)
                    .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "http://fakeurl.com/appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "http://fakeurl.com/appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate.minusDays(30), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "http://fakeurl.com/appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate, null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "http://fakeurl.com/geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, fixedDate.minusYears(1), fixedDate, fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "http://fakeurl.com/geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "http://fakeurl.com/geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(2), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "http://fakeurl.com/geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(1), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                insertInto(TAG)
                    .columns(TAG.ID, TAG.NAME)
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                insertInto(PODCAST_TAGS)
                    .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")),

                insertInto(PLAYLIST)
                    .columns(PLAYLIST.ID, PLAYLIST.NAME, PLAYLIST.COVER_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist", fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind", fromString("51a51b22-4a6f-45df-8905-f5e691eafee4")),

                insertInto(PLAYLIST_ITEMS)
                    .columns(PLAYLIST_ITEMS.PLAYLISTS_ID, PLAYLIST_ITEMS.ITEMS_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
            )
                .execute()
        }


        @Nested
        @DisplayName("ById")
        inner class ById {

            @Test
            fun `on a item deletable from disk`() {
                /* Given */
                val id = fromString("0a774611-c857-44df-b7e0-5e5af31f7b56")

                /* When */
                val response = repository.deleteById(id)

                /* Then */
                assertThat(response).isEqualTo(DeleteItemRequest(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), Path("geekinc.124.mp3"), "Geek Inc HD"))
            }

            @Test
            fun `on a item not deletable from disk`() {
                /* Given */
                val id = fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")

                /* When */
                val response = repository.deleteById(id)

                /* Then */
                assertThat(response).isNull()
            }

            @Test
            fun `and check it remains other items in database`() {
                /* Given */
                val id = fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")

                /* When */
                val response = repository.deleteById(id)

                /* Then */
                assertThat(response).isNull()
                val items = query.select(ITEM.ID).from(ITEM)
                    .fetch { (id) -> id }

                assertThat(items).hasSize(6).contains(
                    fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"),
                    fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"),
                    fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"),
                    fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"),
                    fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"),
                    fromString("0a774611-c867-44df-b7e0-5e5af31f7b56")
                )
            }

            @Test
            fun `an item in a playlist`() {
                /* Given */
                val id = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")

                /* When */
                val response = repository.deleteById(id)

                /* Then */
                assertThat(response).isNull()
            }
        }

    }

    @Nested
    @DisplayName("Should update")
    inner class ShouldUpdateAsDeleted {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                insertInto(COVER)
                    .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                    .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                    .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100),

                insertInto(PODCAST)
                    .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "http://fakeurl.com/appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "http://fakeurl.com/appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate.minusDays(30), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "http://fakeurl.com/appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate, null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "http://fakeurl.com/geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, fixedDate.minusYears(1), fixedDate, fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "http://fakeurl.com/geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "http://fakeurl.com/geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(2), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "http://fakeurl.com/geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(1), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                insertInto(TAG)
                    .columns(TAG.ID, TAG.NAME)
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                insertInto(PODCAST_TAGS)
                    .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")),

                insertInto(PLAYLIST)
                    .columns(PLAYLIST.ID, PLAYLIST.NAME, PLAYLIST.COVER_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist", fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind", fromString("51a51b22-4a6f-45df-8905-f5e691eafee4")),

                insertInto(PLAYLIST_ITEMS)
                    .columns(PLAYLIST_ITEMS.PLAYLISTS_ID, PLAYLIST_ITEMS.ITEMS_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
            )
                .execute()
        }


        @Test
        fun `as deleted`() {
            /* Given */
            val item1 = fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb")
            val item2 = fromString("817a4626-6fd2-457e-8d27-69ea5acdc828")
            val item3 = fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")
            val ids = listOf(item1, item2, item3)
            /* When */
            repository.updateAsDeleted(ids)

            /* Then */
            val items = query.selectFrom(ITEM).where(ITEM.ID.`in`(ids)).fetch()
            assertThat(items).allSatisfy (Consumer {
                assertThat(it.status).isEqualTo(ItemStatus.DELETED)
                assertThat(it.fileName).isNull()
            })
        }

    }

    @Nested
    @DisplayName("Should reset")
    inner class ShouldReset {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                insertInto(COVER)
                    .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                    .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                    .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100),

                insertInto(PODCAST)
                    .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "http://fakeurl.com/appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "http://fakeurl.com/appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate.minusDays(30), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "http://fakeurl.com/appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate, null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "http://fakeurl.com/geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, fixedDate.minusYears(1), fixedDate, fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "http://fakeurl.com/geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "http://fakeurl.com/geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(2), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "http://fakeurl.com/geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(1), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                insertInto(TAG)
                    .columns(TAG.ID, TAG.NAME)
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                insertInto(PODCAST_TAGS)
                    .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")),

                insertInto(PLAYLIST)
                    .columns(PLAYLIST.ID, PLAYLIST.NAME, PLAYLIST.COVER_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist", fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind", fromString("51a51b22-4a6f-45df-8905-f5e691eafee4")),

                insertInto(PLAYLIST_ITEMS)
                    .columns(PLAYLIST_ITEMS.PLAYLISTS_ID, PLAYLIST_ITEMS.ITEMS_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
            )
                .execute()
        }


        @Test
        fun `by Id`() {
            /* Given */
            val id = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")
            /* When */
            val it = repository.resetById(id)!!
            /* Then */
            assertAll {
                assertThat(it.id).isEqualTo(id)
                assertThat(it.title).isEqualTo("Geek INC 126")
                assertThat(it.url).isEqualTo("http://fakeurl.com/geekinc.126.mp3")
                assertThat(it.fileName).isEqualTo(null)
                assertThat(it.fileName).isEqualTo(null)
                assertThat(it.podcast).isEqualTo(Item.Podcast(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss"))
                assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)
                assertThat((it.downloadDate == null)).isEqualTo(true)
            }
            /* And */
            val numberOfFail = query.selectFrom(ITEM).where(ITEM.ID.eq(id)).fetchOne()?.numberOfFail
            assertThat(numberOfFail).isEqualTo(0)
        }

    }

    @Nested
    @DisplayName("should find if item has to be deleted")
    inner class HasToBeDeleted {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                insertInto(COVER)
                    .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                    .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                    .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100),

                insertInto(PODCAST)
                    .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "http://fakeurl.com/appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "http://fakeurl.com/appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate.minusDays(30), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "http://fakeurl.com/appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate, null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "http://fakeurl.com/geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, fixedDate.minusYears(1), fixedDate, fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "http://fakeurl.com/geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "http://fakeurl.com/geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(2), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "http://fakeurl.com/geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(1), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                insertInto(TAG)
                    .columns(TAG.ID, TAG.NAME)
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                insertInto(PODCAST_TAGS)
                    .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")),

                insertInto(PLAYLIST)
                    .columns(PLAYLIST.ID, PLAYLIST.NAME, PLAYLIST.COVER_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist", fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind", fromString("51a51b22-4a6f-45df-8905-f5e691eafee4")),

                insertInto(PLAYLIST_ITEMS)
                    .columns(PLAYLIST_ITEMS.PLAYLISTS_ID, PLAYLIST_ITEMS.ITEMS_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
            )
                .execute()
        }

        @Test
        fun `and return true because its parent podcast has to`() {
            /* Given */
            val id = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")
            /* When */
            val hasToBeDeleted = repository.hasToBeDeleted(id)
            /* Then */
            assertThat(hasToBeDeleted).isTrue()
        }

        @Test
        fun `and return false because its parent podcast hasn't`() {
            /* Given */
            val id = fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")
            /* When */
            val hasToBeDeleted = repository.hasToBeDeleted(id)

            /* Then */
            assertThat(hasToBeDeleted).isFalse()
        }


    }

    @Nested
    @DisplayName("should search with pagination")
    inner class ShouldSearchWithPagination {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                insertInto(COVER)
                    .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/Appload/cover.png", 100, 100)
                    .values(fromString("4b240b0a-516b-42e9-b9fc-e49b5f868045"), "http://fake.url.com/geekinchd/cover.png", 100, 100)
                    .values(fromString("a8eb1ea2-354c-4a8e-931a-dc0286a2a66e"), "http://fake.url.com/foopodcast/cover.png", 100, 100)
                    .values(fromString("8eac2413-3732-4c40-9c80-03e166dba3f0"), "http://fake.url.com/otherpodcast/cover.png", 100, 100)

                    .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                    .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100),

                insertInto(PODCAST)
                    .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Appload", "http://fake.url.com/appload/rss", "YOUTUBE", true)
                    .values(fromString("ccb75276-7a8c-4da9-b4fd-27ccec075c65"), "Geek Inc HD", "http://fake.url.com/geekinchd/rss", "YOUTUBE", true)
                    .values(fromString("cfb8c605-7e10-43b1-9b40-41ee8b5b13d3"), "Foo podcast", "http://fake.url.com/foo/rss", "YOUTUBE", true)
                    .values(fromString("4dc2ccef-42ab-4733-8945-e3f2849b8083"), "Other Podcast", "http://fake.url.com/other/rss", "YOUTUBE", true),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .apply {
                        val max = 50
                        (1..max).forEach { val idx = max - it + 1; values(UUID.randomUUID(), "Appload $idx", "http://fakeurl.com/appload.$idx.mp3", "http://fakeurl.com/appload.$idx.mp3", Path("appload.$idx.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, fixedDate.minusDays(it.toLong()), fixedDate.minusDays(it.toLong()+1), fixedDate.minusDays(15.toLong()+2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "audio/mp3") }
                        (1..max).forEach { val idx = max - it + 1; values(UUID.randomUUID(), "Geek Inc HD $idx", "http://fakeurl.com/geekinchd.$idx.mp3", "http://fakeurl.com/geekinchd.$idx.mp3", Path("geekinchd.$idx.mp3"), fromString("ccb75276-7a8c-4da9-b4fd-27ccec075c65"), ItemStatus.FINISH, fixedDate.minusDays(it.toLong()), fixedDate.minusDays(it.toLong()+1), fixedDate.minusDays(15.toLong()+2), 0, fromString("4b240b0a-516b-42e9-b9fc-e49b5f868045"), "desc", "video/mp4") }
                        (1..max).forEach { val idx = max - it + 1; values(UUID.randomUUID(), "Foo podcast $idx", "http://fakeurl.com/foo.$idx.mp3", "http://fakeurl.com/foo.$idx.mp3", Path("foo.$idx.mp3"), fromString("cfb8c605-7e10-43b1-9b40-41ee8b5b13d3"), ItemStatus.FINISH, fixedDate.minusDays(it.toLong()), fixedDate.minusDays(it.toLong()+1), fixedDate.minusDays(15.toLong()+2), 0, fromString("a8eb1ea2-354c-4a8e-931a-dc0286a2a66e"), "desc", "unknown/unknown") }
                        (1..max).forEach { val idx = max - it + 1; values(UUID.randomUUID(), "Other Podcast $idx", "http://fakeurl.com/other.$idx.mp3", "http://fakeurl.com/other.$idx.mp3", Path("other.$idx.mp3"), fromString("4dc2ccef-42ab-4733-8945-e3f2849b8083"), ItemStatus.NOT_DOWNLOADED, fixedDate.minusDays(it.toLong()), fixedDate.minusDays(it.toLong()+1), fixedDate.minusDays(15.toLong()+2), 0, fromString("8eac2413-3732-4c40-9c80-03e166dba3f0"), "desc with content", "video/webm") }
                    },

                insertInto(TAG)
                    .columns(TAG.ID, TAG.NAME)
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "T1")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "T2")
                    .values(fromString("6936b895-0de6-43f6-acaa-678511d3c37b"), "T3")
                    .values(fromString("4cff2eb7-6398-43cf-96b7-f5699377fdb4"), "T4"),

                insertInto(PODCAST_TAGS)
                    .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))
                    .values(fromString("ccb75276-7a8c-4da9-b4fd-27ccec075c65"), fromString("6936b895-0de6-43f6-acaa-678511d3c37b"))
                    .values(fromString("4dc2ccef-42ab-4733-8945-e3f2849b8083"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
            )
                .execute()
        }

        @Nested
        @DisplayName("wth specific order")
        inner class WithSpecificOrder {

            @Test
            fun `with downloadDate asc`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("asc", "downloadDate"))

                /* When */
                val it = repository.search("", listOf(), listOf(), page, null)

                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
                    assertThat(it.number).isEqualTo(0)
                    assertThat(it.numberOfElements).isEqualTo(12)
                    assertThat(it.totalElements).isEqualTo(200)
                    assertThat(it.totalPages).isEqualTo(17)
                    assertThat(it.content.map(Item::title)).contains(
                        "Appload 1", "Geek Inc HD 1", "Foo podcast 1", "Other Podcast 1",
                        "Appload 2", "Geek Inc HD 2", "Foo podcast 2", "Other Podcast 2",
                        "Appload 3", "Geek Inc HD 3", "Foo podcast 3", "Other Podcast 3"
                    )
                }
            }

            @Test
            fun `with pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf(), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
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
            }
        }

        @Nested
        @DisplayName("on query parameter")
        inner class OnQueryParameter {

            @Test
            fun empty() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf(), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `with query matching item title`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("podcast", listOf(), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
                    assertThat(it.number).isEqualTo(0)
                    assertThat(it.numberOfElements).isEqualTo(12)
                    assertThat(it.totalElements).isEqualTo(100)
                    assertThat(it.totalPages).isEqualTo(9)
                    assertThat(it.content.map(Item::title)).contains(
                        "Other Podcast 50", "Foo podcast 50",
                        "Other Podcast 49", "Foo podcast 49",
                        "Other Podcast 48", "Foo podcast 48",
                        "Other Podcast 47", "Foo podcast 47",
                        "Other Podcast 46", "Foo podcast 46",
                        "Other Podcast 45", "Foo podcast 45"
                    )
                }
            }

            @Test
            fun `with query matching item description`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("content", listOf(), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
                    assertThat(it.number).isEqualTo(0)
                    assertThat(it.numberOfElements).isEqualTo(12)
                    assertThat(it.totalElements).isEqualTo(50)
                    assertThat(it.totalPages).isEqualTo(5)
                    assertThat(it.content.map(Item::title)).contains(
                        "Other Podcast 50", "Other Podcast 49",
                        "Other Podcast 48", "Other Podcast 47",
                        "Other Podcast 46", "Other Podcast 45",
                        "Other Podcast 44", "Other Podcast 43",
                        "Other Podcast 42", "Other Podcast 41",
                        "Other Podcast 40", "Other Podcast 39"
                    )
                }
            }


        }

        @Nested
        @DisplayName("with no tags and no specific statuses")
        inner class WithNoTagsAndNoStatutes {

            @Test
            fun `for first page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(0, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf(), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for second page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(1, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf(), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for before last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(15, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf(), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(16, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf(), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(8)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isTrue
                    assertThat(it.number).isEqualTo(16)
                    assertThat(it.numberOfElements).isEqualTo(8)
                    assertThat(it.totalElements).isEqualTo(200)
                    assertThat(it.totalPages).isEqualTo(17)
                    assertThat(it.content.map(Item::title)).contains(
                        "Appload 2", "Geek Inc HD 2", "Foo podcast 2", "Other Podcast 2",
                        "Appload 1", "Geek Inc HD 1", "Foo podcast 1", "Other Podcast 1"
                    )
                }
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
                val it = repository.search("", listOf("T1"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for second page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(1, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf("T1"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for before last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(7, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf("T1"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(8, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf("T1"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(4)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isTrue
                    assertThat(it.number).isEqualTo(8)
                    assertThat(it.numberOfElements).isEqualTo(4)
                    assertThat(it.totalElements).isEqualTo(100)
                    assertThat(it.totalPages).isEqualTo(9)
                    assertThat(it.content.map(Item::title)).contains(
                        "Appload 2", "Other Podcast 2", "Appload 1", "Other Podcast 1"
                    )
                }
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
                val it = repository.search("", listOf("T1", "T2"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for second page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(1, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf("T1", "T2"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for before last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(3, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf("T1", "T2"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(4, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf("T1", "T2"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(2)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isTrue
                    assertThat(it.number).isEqualTo(4)
                    assertThat(it.numberOfElements).isEqualTo(2)
                    assertThat(it.totalElements).isEqualTo(50)
                    assertThat(it.totalPages).isEqualTo(5)
                    assertThat(it.content.map(Item::title)).contains(
                        "Appload 2", "Appload 1"
                    )
                }
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
                val it = repository.search("", listOf("T3"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for second page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(1, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf("T3"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for before last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(3, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf("T3"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(4, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf("T3"), listOf(), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(2)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isTrue
                    assertThat(it.number).isEqualTo(4)
                    assertThat(it.numberOfElements).isEqualTo(2)
                    assertThat(it.totalElements).isEqualTo(50)
                    assertThat(it.totalPages).isEqualTo(5)
                    assertThat(it.content.map(Item::title))
                        .contains("Geek Inc HD 2", "Geek Inc HD 1")
                }
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
                val it = repository.search("", listOf(), listOf(Status.NOT_DOWNLOADED), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for second page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(1, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf(), listOf(Status.NOT_DOWNLOADED), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for before last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(3, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf(), listOf(Status.NOT_DOWNLOADED), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isFalse
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
            }

            @Test
            fun `for last page with 12 elements, pubdate desc`() {
                /* Given */
                val page = ItemPageRequest(4, 12, ItemSort("desc", "pubDate"))

                /* When */
                val it = repository.search("", listOf(), listOf(Status.NOT_DOWNLOADED), page, null)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(2)
                    assertThat(it.first).isFalse
                    assertThat(it.last).isTrue
                    assertThat(it.number).isEqualTo(4)
                    assertThat(it.numberOfElements).isEqualTo(2)
                    assertThat(it.totalElements).isEqualTo(50)
                    assertThat(it.totalPages).isEqualTo(5)
                    assertThat(it.content.map(Item::title))
                        .contains("Other Podcast 2", "Other Podcast 1")
                }
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
                val it = repository.search("", listOf(), listOf(), page, podcastId)
                /* Then */
                assertAll {
                    assertThat(it.content.size).isEqualTo(12)
                    assertThat(it.first).isTrue
                    assertThat(it.last).isFalse
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
            }
        }
    }

    @Nested
    @DisplayName("should create")
    inner class ShouldCreate {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                insertInto(COVER)
                    .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                    .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                    .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100),

                insertInto(PODCAST)
                    .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED, PODCAST.COVER_ID)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68")),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate.minusDays(30), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate, null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, fixedDate.minusYears(1), fixedDate, fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(2), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(1), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                insertInto(TAG)
                    .columns(TAG.ID, TAG.NAME)
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                insertInto(PODCAST_TAGS)
                    .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")),

                insertInto(PLAYLIST)
                    .columns(PLAYLIST.ID, PLAYLIST.NAME, PLAYLIST.COVER_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist", fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind", fromString("51a51b22-4a6f-45df-8905-f5e691eafee4")),

                insertInto(PLAYLIST_ITEMS)
                    .columns(PLAYLIST_ITEMS.PLAYLISTS_ID, PLAYLIST_ITEMS.ITEMS_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
            )
                .execute()
        }

        @Nested
        @DisplayName("a single item")
        inner class ASingleItem {

            @Nested
            @DisplayName("with success")
            inner class WithSuccess {

                @Test
                fun `without any specificities`() {
                    /* Given */
                    val item = ItemForCreation(
                        title = "an item",
                        url = "http://foo.bar.com/an_item",
                        guid = "http://foo.bar.com/an_item",

                        pubDate = now(),
                        downloadDate = now(),
                        creationDate = now(),

                        description = "a description",
                        mimeType = "audio/mp3",
                        length = 1234,
                        fileName = Path("ofejeaoijefa.mp3"),
                        status = Status.FINISH,

                        podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                        cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/item.jpg"))
                    )
                    val numberOfItem = query.selectCount().from(ITEM).fetchOne(count()) ?: 0
                    val numberOfCover = query.selectCount().from(COVER).fetchOne(count()) ?: 0

                    /* When */
                    val it = repository.create(item)!!

                    /* Then */
                    assertAll {
                        assertThat(it.title).isEqualTo("an item")
                        assertThat(it.url).isEqualTo("http://foo.bar.com/an_item")
                        assertThat(it.pubDate).isCloseTo(now(), within(10, SECONDS))
                        assertThat(it.downloadDate).isCloseTo(now(), within(10, SECONDS))
                        assertThat(it.creationDate).isCloseTo(now(), within(10, SECONDS))
                        assertThat(it.description).isEqualTo("a description")
                        assertThat(it.mimeType).isEqualTo("audio/mp3")
                        assertThat(it.length).isEqualTo(1234)
                        assertThat(it.fileName).isEqualTo(Path("ofejeaoijefa.mp3"))
                        assertThat(it.status).isEqualTo(Status.FINISH)

                        assertThat(it.podcast.id).isEqualTo(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"))
                        assertThat(it.podcast.title).isEqualTo("Geek Inc HD")
                        assertThat(it.podcast.url).isEqualTo("http://fake.url.com/rss")

                        assertThat(it.cover.height).isEqualTo(100)
                        assertThat(it.cover.width).isEqualTo(100)
                        assertThat(it.cover.url).isEqualTo(URI("http://foo.bar.com/cover/item.jpg"))

                        assertThat(numberOfItem + 1).isEqualTo(query.selectCount().from(ITEM).fetchOne(count()))
                        assertThat(numberOfCover + 1).isEqualTo(query.selectCount().from(COVER).fetchOne(count()))
                    }

                }

                @Test
                fun `with dollar in text fields`() {
                    /* Given */
                    val item = ItemForCreation(
                        title = "$1 item",
                        url = "http://foo.bar.com/an_item",
                        guid = "http://foo.bar.com/an_item",

                        pubDate = now(),
                        downloadDate = now(),
                        creationDate = now(),

                        description = "it costs $1",
                        mimeType = "$1/mp3",
                        length = 1234,
                        fileName = Path("$1.mp3"),
                        status = Status.FINISH,

                        podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                        cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/item.jpg"))
                    )
                    val numberOfItem = query.selectCount().from(ITEM).fetchOne(count())!!
                    val numberOfCover = query.selectCount().from(COVER).fetchOne(count())!!

                    /* When */
                    val it = repository.create(item)!!

                    /* Then */
                    assertAll {
                        assertThat(it.title).isEqualTo("$1 item")
                        assertThat(it.url).isEqualTo("http://foo.bar.com/an_item")
                        assertThat(it.pubDate).isCloseTo(now(), within(10, SECONDS))
                        assertThat(it.downloadDate).isCloseTo(now(), within(10, SECONDS))
                        assertThat(it.creationDate).isCloseTo(now(), within(10, SECONDS))
                        assertThat(it.description).isEqualTo("it costs $1")
                        assertThat(it.mimeType).isEqualTo("$1/mp3")
                        assertThat(it.length).isEqualTo(1234)
                        assertThat(it.fileName).isEqualTo(Path("$1.mp3"))
                        assertThat(it.status).isEqualTo(Status.FINISH)

                        assertThat(it.podcast.id).isEqualTo(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"))
                        assertThat(it.podcast.title).isEqualTo("Geek Inc HD")
                        assertThat(it.podcast.url).isEqualTo("http://fake.url.com/rss")

                        assertThat(it.cover.height).isEqualTo(100)
                        assertThat(it.cover.width).isEqualTo(100)
                        assertThat(it.cover.url).isEqualTo(URI("http://foo.bar.com/cover/item.jpg"))

                        assertThat(numberOfItem + 1).isEqualTo(query.selectCount().from(ITEM).fetchOne(count()))
                        assertThat(numberOfCover + 1).isEqualTo(query.selectCount().from(COVER).fetchOne(count()))
                    }

                }

                @Test
                fun `but found an already existing item with same url, so doesn't do anything and return empty`() {
                    /* Given */
                    val item = ItemForCreation(
                        title = "an item",
                        url = "http://fakeurl.com/geekinc.123.mp3",
                        guid = "http://fakeurl.com/geekinc.123.mp3",

                        pubDate = now(),
                        downloadDate = now(),
                        creationDate = now(),

                        description = "a description",
                        mimeType = "audio/mp3",
                        length = 1234,
                        fileName = Path("ofejeaoijefa.mp3"),
                        status = Status.FINISH,

                        podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                        cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/item.jpg"))
                    )
                    val numberOfItem = query.selectCount().from(ITEM).fetchOne(count())
                    val numberOfCover = query.selectCount().from(COVER).fetchOne(count())
                    /* When */
                    val it = repository.create(item)

                    /* Then */
                    assertThat(it).isNull()
                    assertThat(numberOfItem).isEqualTo(query.selectCount().from(ITEM).fetchOne(count()))
                    assertThat(numberOfCover).isEqualTo(query.selectCount().from(COVER).fetchOne(count()))
                }

                @Test
                fun `but found an already existing item with same guid, so updates url with the new one`() {
                    /* Given */
                    val item = ItemForCreation(
                        title = "another title",
                        url = "http://another-url.com/geekinc.123.mp3",
                        guid = "geekinc.123.mp3",

                        pubDate = now(),
                        downloadDate = now(),
                        creationDate = now(),

                        description = "another description",
                        mimeType = "audio/mp3",
                        length = 1234,
                        fileName = Path("ofejeaoijefa.mp3"),
                        status = Status.FINISH,

                        podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                        cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/item.jpg"))
                    )
                    val numberOfItem = query.selectCount().from(ITEM).fetchOne(count())
                    val numberOfCover = query.selectCount().from(COVER).fetchOne(count())

                    /* When */
                    val it = repository.create(item)

                    /* Then */
                    assertThat(it).isNull()

                    assertThat(numberOfItem).isEqualTo(query.selectCount().from(ITEM).fetchOne(count()))
                    assertThat(numberOfCover).isEqualTo(query.selectCount().from(COVER).fetchOne(count()))

                    val updatedItem = query.selectFrom(ITEM)
                        .where(ITEM.GUID.eq("geekinc.123.mp3"))
                        .fetch()
                        .firstOrNull() ?: error("item not found")

                    assertAll {
                        assertThat(updatedItem[ITEM.URL]).isEqualTo("http://another-url.com/geekinc.123.mp3")
                        assertThat(updatedItem[ITEM.TITLE]).isEqualTo("another title")
                        assertThat(updatedItem[ITEM.DESCRIPTION]).isEqualTo("another description")
                    }
                }


                @Test
                fun `a simple item with download date null`() {
                    /* Given */
                    val now = now()
                    val item = ItemForCreation(
                        title = "an item",
                        url = "http://foo.bar.com/an_item",
                        guid = "http://foo.bar.com/an_item",

                        pubDate = now,
                        downloadDate = null,
                        creationDate = now,

                        description = "a description",
                        mimeType = "audio/mp3",
                        length = 1234,
                        fileName = Path("ofejeaoijefa.mp3"),
                        status = Status.FINISH,

                        podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                        cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/item.jpg"))
                    )
                    val numberOfItem = query.selectCount().from(ITEM).fetchOne(count()) ?: 0
                    val numberOfCover = query.selectCount().from(COVER).fetchOne(count()) ?: 0

                    /* When */
                    val it = repository.create(item)!!

                    /* Then */
                    assertAll {
                        assertThat(it.title).isEqualTo("an item")
                        assertThat(it.url).isEqualTo("http://foo.bar.com/an_item")
                        assertThat(it.pubDate).isCloseTo(now, within(1, SECONDS))
                        assertThat(it.downloadDate).isNull()
                        assertThat(it.creationDate).isCloseTo(now, within(1, SECONDS))
                        assertThat(it.description).isEqualTo("a description")
                        assertThat(it.mimeType).isEqualTo("audio/mp3")
                        assertThat(it.length).isEqualTo(1234)
                        assertThat(it.fileName).isEqualTo(Path("ofejeaoijefa.mp3"))
                        assertThat(it.status).isEqualTo(Status.FINISH)

                        assertThat(it.podcast.id).isEqualTo(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"))
                        assertThat(it.podcast.title).isEqualTo("Geek Inc HD")
                        assertThat(it.podcast.url).isEqualTo("http://fake.url.com/rss")

                        assertThat(it.cover.height).isEqualTo(100)
                        assertThat(it.cover.width).isEqualTo(100)
                        assertThat(it.cover.url).isEqualTo(URI("http://foo.bar.com/cover/item.jpg"))
                    }

                    assertThat(numberOfItem + 1).isEqualTo(query.selectCount().from(ITEM).fetchOne(count()))
                    assertThat(numberOfCover + 1).isEqualTo(query.selectCount().from(COVER).fetchOne(count()))
                }

                @Test
                fun `an item without cover, so it has to fallback to podcast cover`() {
                    /* Given */
                    val now = now()
                    val item = ItemForCreation(
                        title = "an item",
                        url = "http://foo.bar.com/an_item",
                        guid = "http://foo.bar.com/an_item",

                        pubDate = now,
                        downloadDate = now(),
                        creationDate = now,

                        description = "a description",
                        mimeType = "audio/mp3",
                        length = 1234,
                        fileName = Path("ofejeaoijefa.mp3"),
                        status = Status.NOT_DOWNLOADED,

                        podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                        cover = null
                    )
                    val numberOfItem = query.selectCount().from(ITEM).fetchOne(count()) ?: 0
                    val numberOfCover = query.selectCount().from(COVER).fetchOne(count()) ?: 0

                    /* When */
                    val it = repository.create(item)!!

                    /* Then */
                    assertAll {
                        assertThat(it.title).isEqualTo("an item")
                        assertThat(it.url).isEqualTo("http://foo.bar.com/an_item")
                        assertThat(it.pubDate).isCloseTo(now, within(1, SECONDS))
                        assertThat(it.downloadDate).isCloseTo(now, within(1, SECONDS))
                        assertThat(it.creationDate).isCloseTo(now, within(1, SECONDS))
                        assertThat(it.description).isEqualTo("a description")
                        assertThat(it.mimeType).isEqualTo("audio/mp3")
                        assertThat(it.length).isEqualTo(1234)
                        assertThat(it.fileName).isEqualTo(Path("ofejeaoijefa.mp3"))
                        assertThat(it.status).isEqualTo(Status.NOT_DOWNLOADED)

                        assertThat(it.podcast.id).isEqualTo(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"))
                        assertThat(it.podcast.title).isEqualTo("Geek Inc HD")
                        assertThat(it.podcast.url).isEqualTo("http://fake.url.com/rss")

                        assertThat(it.cover.height).isEqualTo(100)
                        assertThat(it.cover.width).isEqualTo(100)
                        assertThat(it.cover.url).isEqualTo(URI("http://fake.url.com/geekinc/cover.png"))
                    }

                    assertThat(numberOfItem + 1).isEqualTo(query.selectCount().from(ITEM).fetchOne(count()))
                    assertThat(numberOfCover + 1).isEqualTo(query.selectCount().from(COVER).fetchOne(count()))
                }
            }

            @Nested
            @DisplayName("with error")
            inner class WithError {

                @Test
                fun `but fail because mimetype is empty`() {
                    /* Given */
                    val item = ItemForCreation(
                        title = "an item",
                        url = "http://foo.bar.com/an_item",
                        guid = "http://foo.bar.com/an_item",

                        pubDate = now(),
                        downloadDate = now(),
                        creationDate = now(),

                        description = "a description",
                        mimeType = "",
                        length = 1234,
                        fileName = Path("ofejeaoijefa.mp3"),
                        status = Status.FINISH,

                        podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                        cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/item.jpg"))
                    )

                    /* When */
                    assertThatThrownBy { repository.create(item) }

                        /* Then */
                        .isInstanceOf(DataIntegrityViolationException::class.java)

                }

                @Test
                fun `but fail because mimetype does not contain slash`() {
                    /* Given */
                    val item = ItemForCreation(
                        title = "an item",
                        url = "http://foo.bar.com/an_item",
                        guid = "http://foo.bar.com/an_item",

                        pubDate = now(),
                        downloadDate = now(),
                        creationDate = now(),

                        description = "a description",
                        mimeType = "foo",
                        length = 1234,
                        fileName = Path("ofejeaoijefa.mp3"),
                        status = Status.FINISH,

                        podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                        cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/item.jpg"))
                    )

                    /* When */
                    assertThatThrownBy { repository.create(item) }

                        /* Then */
                        .isInstanceOf(DataIntegrityViolationException::class.java)
                }

            }

        }

        @Nested
        @DisplayName("multiple items")
        inner class MultipleItems {
            val item1 = ItemForCreation(
                title = "one",
                url = "http://foo.bar.com/1",
                guid = "http://foo.bar.com/1",

                pubDate = now(),
                downloadDate = now(),
                creationDate = now(),

                description = "a description",
                mimeType = "audio/mp3",
                length = 1234,
                fileName = Path("1.mp3"),
                status = Status.FINISH,

                podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/1.jpg"))
            )
            val item2 = ItemForCreation(
                title = "two",
                url = "http://foo.bar.com/2",
                guid = "http://foo.bar.com/2",

                pubDate = now(),
                downloadDate = now(),
                creationDate = now(),

                description = "a description",
                mimeType = "audio/mp3",
                length = 1234,
                fileName = Path("2.mp3"),
                status = Status.FINISH,

                podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/2.jpg"))
            )
            val item3 = ItemForCreation(
                title = "three",
                url = "http://foo.bar.com/3",
                guid = "http://foo.bar.com/3",

                pubDate = now(),
                downloadDate = now(),
                creationDate = now(),

                description = "a description",
                mimeType = "audio/mp3",
                length = 1234,
                fileName = Path("3.mp3"),
                status = Status.FINISH,

                podcastId = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                cover = CoverForCreation(100, 100, URI("http://foo.bar.com/cover/3.jpg"))
            )

            @Nested
            @DisplayName("with success")
            inner class WithSuccess {

                @Test
                fun `with no specificities`() {
                    /* Given */
                    val numberOfItem = query.selectCount().from(ITEM).fetchOne(count())!!
                    val numberOfCover = query.selectCount().from(COVER).fetchOne(count())!!

                    /* When */
                    val items = repository.create(listOf(item1, item2, item3))

                    /* Then */
                    assertThat(items).hasSize(3)

                    assertThat(numberOfItem + 3).isEqualTo(query.selectCount().from(ITEM).fetchOne(count()))
                    assertThat(numberOfCover + 3).isEqualTo(query.selectCount().from(COVER).fetchOne(count()))
                }
            }
        }



    }

    @Nested
    @DisplayName("should find item in downloading state")
    inner class ShouldFindItemInDownloadingState {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                insertInto(COVER)
                    .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                    .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                    .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100),

                insertInto(PODCAST)
                    .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "http://fakeurl.com/appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "http://fakeurl.com/appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate.minusDays(30), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "http://fakeurl.com/appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate, null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "http://fakeurl.com/geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, fixedDate.minusYears(1), fixedDate, fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "http://fakeurl.com/geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "http://fakeurl.com/geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(2), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "http://fakeurl.com/geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(1), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                insertInto(TAG)
                    .columns(TAG.ID, TAG.NAME)
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                insertInto(PODCAST_TAGS)
                    .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")),

                insertInto(PLAYLIST)
                    .columns(PLAYLIST.ID, PLAYLIST.NAME, PLAYLIST.COVER_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist", fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind", fromString("51a51b22-4a6f-45df-8905-f5e691eafee4")),

                insertInto(PLAYLIST_ITEMS)
                    .columns(PLAYLIST_ITEMS.PLAYLISTS_ID, PLAYLIST_ITEMS.ITEMS_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56")),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .values(fromString("0a774612-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 140", "http://fakeurl.com/geekinc.140.mp3", "http://fakeurl.com/geekinc.140.mp3", Path("geekinc.140.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.STARTED, fixedDate.minusDays(15), fixedDate.minusDays(15), fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774613-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 141", "http://fakeurl.com/geekinc.141.mp3", "http://fakeurl.com/geekinc.141.mp3", Path("geekinc.141.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.PAUSED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(2), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a674614-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 142", "http://fakeurl.com/geekinc.142.mp3", "http://fakeurl.com/geekinc.142.mp3", Path("geekinc.142.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.STARTED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(1), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
            )
                .execute()
        }

        @Test
        fun `with success`() {
            /* Given */
            val ids = listOf(
                fromString("0a774612-c857-44df-b7e0-5e5af31f7b56"),
                fromString("0a774613-c867-44df-b7e0-5e5af31f7b56"),
                fromString("0a674614-c867-44df-b7e0-5e5af31f7b56")
            )
            /* When */
            repository.resetItemWithDownloadingState()

            /* Then */
            val statuses = query.selectFrom(ITEM).where(ITEM.ID.`in`(ids))

                .fetch()
                .map { it[ITEM.STATUS] }

            val others = query
                .selectFrom(ITEM).where(ITEM.ID.notIn(ids)).orderBy(ITEM.ID.asc())

                .fetch()
                .mapNotNull { it[ITEM.STATUS] }.toSet()

            assertThat(statuses).containsOnly(ItemStatus.NOT_DOWNLOADED)
            assertThat(others).containsOnly(ItemStatus.FINISH, ItemStatus.NOT_DOWNLOADED, ItemStatus.DELETED, ItemStatus.FINISH, ItemStatus.FAILED)
        }

    }

    @Nested
    @DisplayName("should find all playlists containing an item by id")
    inner class ShouldFindAllPlaylistsContainingAnItemById {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                insertInto(COVER)
                    .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                    .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                    .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                    .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                    .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100),

                insertInto(PODCAST)
                    .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true),

                insertInto(ITEM)
                    .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)
                    .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "http://fakeurl.com/appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "http://fakeurl.com/appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate.minusDays(30), null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "http://fakeurl.com/appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, fixedDate, null, null, 0, fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                    .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "http://fakeurl.com/geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, fixedDate.minusYears(1), fixedDate, fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "http://fakeurl.com/geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, fixedDate.minusDays(15), fixedDate.minusDays(15), fixedDate.minusMonths(2), 0, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "http://fakeurl.com/geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(2), 3, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                    .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "http://fakeurl.com/geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, fixedDate.minusDays(1), null, fixedDate.minusWeeks(1), 7, fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                insertInto(TAG)
                    .columns(TAG.ID, TAG.NAME)
                    .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                    .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                insertInto(PODCAST_TAGS)
                    .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                    .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                    .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6")),

                insertInto(PLAYLIST)
                    .columns(PLAYLIST.ID, PLAYLIST.NAME, PLAYLIST.COVER_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist", fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind", fromString("51a51b22-4a6f-45df-8905-f5e691eafee4")),

                insertInto(PLAYLIST_ITEMS)
                    .columns(PLAYLIST_ITEMS.PLAYLISTS_ID, PLAYLIST_ITEMS.ITEMS_ID)
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
                    .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"))
            )
                .execute()
        }


        @Test
        fun `and find no playlists because nothing is associated to this item`() {
            /* Given */
            val uuid = fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb")
            /* When */
            val items = repository.findPlaylistsContainingItem(uuid)
            /* Then */
            assertThat(items).isEmpty()
        }

        @Test
        fun `and find one playlist`() {
            /* Given */
            val uuid = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")
            /* When */
            val items = repository.findPlaylistsContainingItem(uuid)
            /* Then */
            assertThat(items)
                .hasSize(1)
                .containsOnly(
                    ItemPlaylist(
                        id = fromString("dc024a30-bd02-11e5-a837-0800200c9a66"),
                        name = "Humour Playlist"
                    )
                )
        }

        @Test
        fun `and find many playlists`() {
            /* Given */
            val uuid = fromString("0a774611-c867-44df-b7e0-5e5af31f7b56")
            /* When */
            val items = repository.findPlaylistsContainingItem(uuid)

            /* Then */
            assertThat(items).containsOnly(
                ItemPlaylist(
                    id = fromString("24248480-bd04-11e5-a837-0800200c9a66"),
                    name = "Conférence Rewind"
                ),

                ItemPlaylist(
                    id = fromString("dc024a30-bd02-11e5-a837-0800200c9a66"),
                    name = "Humour Playlist"
                )
            )
        }
    }
}
