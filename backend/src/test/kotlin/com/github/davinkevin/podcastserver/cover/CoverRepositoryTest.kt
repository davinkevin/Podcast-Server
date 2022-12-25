package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.JooqR2DBCTest
import com.github.davinkevin.podcastserver.cover.DeleteCoverRequest.Item
import com.github.davinkevin.podcastserver.cover.DeleteCoverRequest.Podcast
import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.database.enums.ItemStatus
import com.github.davinkevin.podcastserver.r2dbc
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.impl.DSL.insertInto
import org.jooq.impl.DSL.truncate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Propagation.NEVER
import org.springframework.transaction.annotation.Transactional
import reactor.test.StepVerifier
import java.net.URI
import java.time.OffsetDateTime.now
import java.util.UUID.fromString
import kotlin.io.path.Path

/**
 * Created by kevin on 14/09/2019
 */
@JooqR2DBCTest
@Transactional(propagation = NEVER)
@Import(CoverRepository::class)
class CoverRepositoryTest(
        @Autowired val repository: CoverRepository,
        @Autowired val query: DSLContext
) {

    @BeforeEach
    fun beforeEach() {
        query.batch(
                truncate(PODCAST_TAGS).cascade(),
                truncate(TAG).cascade(),
                truncate(ITEM).cascade(),
                truncate(PODCAST).cascade(),
                truncate(COVER).cascade(),
        ).r2dbc().execute()
    }

    @Nested
    @DisplayName("should save cover")
    inner class ShouldSaveCover {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                    insertInto(COVER)
                            .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                            .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                            .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                            .values(fromString("1ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                            .values(fromString("2ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                            .values(fromString("3ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                            .values(fromString("1f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                            .values(fromString("2f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                            .values(fromString("3f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                            .values(fromString("4f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100),

                    insertInto(PODCAST)
                            .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                            .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                            .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true),

                    insertInto(ITEM)
                            .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)

                            .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "http://fakeurl.com/appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, now().minusDays(15), now().minusDays(15), now().minusDays(100), 0, fromString("1ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                            .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "http://fakeurl.com/appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, now().minusDays(30), null, now().minusDays(100), 0, fromString("2ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                            .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "http://fakeurl.com/appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, now(), null, now().minusDays(100), 0, fromString("3ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")

                            .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "http://fakeurl.com/geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, now().minusYears(1), now(), now().minusMonths(2), 0, fromString("1f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                            .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "http://fakeurl.com/geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, now().minusDays(15), now().minusDays(15), now().minusMonths(2), 0, fromString("2f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                            .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "http://fakeurl.com/geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, now().minusDays(1), null, now().minusWeeks(2), 3, fromString("3f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                            .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "http://fakeurl.com/geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, now().minusDays(1), null, now().minusWeeks(1), 7, fromString("4f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                    insertInto(TAG)
                            .columns(TAG.ID, TAG.NAME)
                            .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                            .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                    insertInto(PODCAST_TAGS)
                            .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                            .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                            .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))

            ).r2dbc().execute()
        }

        @Test
        fun `with success`() {
            /* Given */
            val url = URI("https//foo.bar.com/cover/image.png")
            val cover = CoverForCreation(
                    width = 100,
                    height = 200,
                    url = url
            )
            /* When */
            StepVerifier.create(repository.save(cover))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isNotNull
                        assertThat(it.width).isEqualTo(100)
                        assertThat(it.height).isEqualTo(200)
                        assertThat(it.url).isEqualTo(url)
                    }
                    .verifyComplete()

            val r = query.selectFrom(COVER).r2dbc().fetch()
            assertThat(r).hasSize(10)

            val coverRecord = r.first { it[COVER.URL] == url.toASCIIString() }
            assertThat(coverRecord.id).isNotNull
            assertThat(coverRecord.width).isEqualTo(100)
            assertThat(coverRecord.height).isEqualTo(200)
            assertThat(coverRecord.url).isEqualTo(url.toASCIIString())
        }

    }

    @Nested
    @DisplayName("should find cover older than")
    inner class ShouldFindCoverOlderThan {

        @BeforeEach
        fun beforeEach() {
            query.batch(
                    insertInto(COVER)
                            .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                            .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                            .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                            .values(fromString("1ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                            .values(fromString("2ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                            .values(fromString("3ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                            .values(fromString("1f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                            .values(fromString("2f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                            .values(fromString("3f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                            .values(fromString("4f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100),

                    insertInto(PODCAST)
                            .columns(PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.TYPE, PODCAST.HAS_TO_BE_DELETED)
                            .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                            .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true),

                    insertInto(ITEM)
                            .columns(ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.FILE_NAME, ITEM.PODCAST_ID, ITEM.STATUS, ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.DESCRIPTION, ITEM.MIME_TYPE)

                            .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "http://fakeurl.com/appload.1.mp3", Path("appload.1.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.FINISH, now().minusDays(15), now().minusDays(15), now().minusDays(100), 0, fromString("1ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                            .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "http://fakeurl.com/appload.2.mp3", Path("appload.2.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, now().minusDays(30), null, now().minusDays(100), 0, fromString("2ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                            .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "http://fakeurl.com/appload.3.mp3", Path("appload.3.mp3"), fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), ItemStatus.NOT_DOWNLOADED, now(), null, now().minusDays(100), 0, fromString("3ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")

                            .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "http://fakeurl.com/geekinc.123.mp3", Path("geekinc.123.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.DELETED, now().minusYears(1), now(), now().minusMonths(2), 0, fromString("1f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                            .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "http://fakeurl.com/geekinc.124.mp3", Path("geekinc.124.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FINISH, now().minusDays(15), now().minusDays(15), now().minusMonths(2), 0, fromString("2f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                            .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "http://fakeurl.com/geekinc.122.mp3", Path("geekinc.122.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, now().minusDays(1), null, now().minusWeeks(2), 3, fromString("3f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                            .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "http://fakeurl.com/geekinc.126.mp3", Path("geekinc.126.mp3"), fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), ItemStatus.FAILED, now().minusDays(1), null, now().minusWeeks(1), 7, fromString("4f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4"),

                    insertInto(TAG)
                            .columns(TAG.ID, TAG.NAME)
                            .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                            .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere"),

                    insertInto(PODCAST_TAGS)
                            .columns(PODCAST_TAGS.PODCASTS_ID, PODCAST_TAGS.TAGS_ID)
                            .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                            .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))

            ).r2dbc().execute()
        }

        @Test
        fun `99999 days and find nothing`() {
            /* Given */
            /* When */
            StepVerifier.create(repository.findCoverOlderThan(now().minusDays(99999)))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()
        }

        @Test
        fun `1 day and find everything`() {
            /* Given */
            /* When */
            StepVerifier.create(repository.findCoverOlderThan(now().minusDays(1)))
                    /* Then */
                    .expectSubscription()
                    .expectNext(
                            DeleteCoverRequest(
                                    id=fromString("1ea0373e-7af6-4e15-b0fd-9ec4b10822ec"),
                                    extension="png",
                                    item=Item(id= fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), title="Appload 1"),
                                    podcast=Podcast(id= fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), title="AppLoad")
                            ),
                            DeleteCoverRequest(
                                    id=fromString("1f050dc4-6a2e-46c3-8276-43098c011e68"),
                                    extension="png",
                                    item=Item(id= fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), title="Geek INC 123"),
                                    podcast=Podcast(id= fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), title="Geek Inc HD")
                            ),
                            DeleteCoverRequest(
                                    id=fromString("2ea0373e-7af6-4e15-b0fd-9ec4b10822ec"),
                                    extension="png",
                                    item=Item(id= fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), title="Appload 2"),
                                    podcast=Podcast(id= fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), title="AppLoad")
                            ),
                            DeleteCoverRequest(
                                    id=fromString("2f050dc4-6a2e-46c3-8276-43098c011e68"),
                                    extension="png",
                                    item=Item(id= fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), title="Geek INC 124"),
                                    podcast=Podcast(id= fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), title="Geek Inc HD")
                            ),
                            DeleteCoverRequest(
                                    id=fromString("3ea0373e-7af6-4e15-b0fd-9ec4b10822ec"),
                                    extension="png",
                                    item=Item(id= fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), title="Appload 3"),
                                    podcast=Podcast(id= fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), title="AppLoad")
                            ),
                            DeleteCoverRequest(
                                    id=fromString("3f050dc4-6a2e-46c3-8276-43098c011e68"),
                                    extension="png",
                                    item=Item(id= fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), title="Geek INC 122"),
                                    podcast=Podcast(id= fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), title="Geek Inc HD")
                            ),
                            DeleteCoverRequest(
                                    id=fromString("4f050dc4-6a2e-46c3-8276-43098c011e68"),
                                    extension="png",
                                    item=Item(id= fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), title="Geek INC 126"),
                                    podcast=Podcast(id= fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), title="Geek Inc HD")
                            )
                    )
                    .verifyComplete()
        }
    }
}
