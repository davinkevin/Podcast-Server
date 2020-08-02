package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation.Item
import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation.Podcast
import com.github.davinkevin.podcastserver.entity.Status
import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.Operations.insertInto
import com.ninja_squad.dbsetup.destination.DataSourceDestination
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import com.github.davinkevin.podcastserver.DELETE_ALL
import com.github.davinkevin.podcastserver.database.tables.Cover.*
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import reactor.test.StepVerifier
import java.net.URI
import java.time.OffsetDateTime.now
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.time.format.DateTimeFormatterBuilder
import java.util.UUID.fromString
import javax.sql.DataSource

/**
 * Created by kevin on 14/09/2019
 */
@JooqTest
@Import(CoverRepository::class)
class CoverRepositoryTest(
    @Autowired val query: DSLContext,
    @Autowired val repository: CoverRepository,
    @Autowired val db: DataSourceDestination
) {

    @Nested
    @DisplayName("should save cover")
    inner class ShouldSaveCover {

        private val operation = sequenceOf(DELETE_ALL, INSERT_COVER_DATA)

        @BeforeEach
        fun beforeEach() = DbSetup(db, operation).launch()

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
            StepVerifier.create(repository.save(cover).log())
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isNotNull()
                        assertThat(it.width).isEqualTo(100)
                        assertThat(it.height).isEqualTo(200)
                        assertThat(it.url).isEqualTo(url)
                    }
                    .verifyComplete()

            val r = query.selectFrom(COVER).fetch()
            assertThat(r).hasSize(10)

            val coverRecord = r.first { it[COVER.URL] == url.toASCIIString() }
            assertThat(coverRecord.id).isNotNull()
            assertThat(coverRecord.width).isEqualTo(100)
            assertThat(coverRecord.height).isEqualTo(200)
            assertThat(coverRecord.url).isEqualTo(url.toASCIIString())

        }

    }
    
    @Nested
    @DisplayName("should find cover older than")
    inner class ShouldFindCoverOlderThan {

        private val operation = sequenceOf(DELETE_ALL, INSERT_COVER_DATA)

        @BeforeEach
        fun beforeEach() = DbSetup(db, operation).launch()

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
                            DeleteCoverInformation(
                                    id=fromString("1ea0373e-7af6-4e15-b0fd-9ec4b10822ec"),
                                    extension="png",
                                    item=Item(id= fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), title="Appload 1"),
                                    podcast=Podcast(id= fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), title="AppLoad")
                            ),
                            DeleteCoverInformation(
                                    id=fromString("1f050dc4-6a2e-46c3-8276-43098c011e68"),
                                    extension="png",
                                    item=Item(id= fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), title="Geek INC 123"),
                                    podcast=Podcast(id= fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), title="Geek Inc HD")
                            ),
                            DeleteCoverInformation(
                                    id=fromString("2ea0373e-7af6-4e15-b0fd-9ec4b10822ec"),
                                    extension="png",
                                    item=Item(id= fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), title="Appload 2"),
                                    podcast=Podcast(id= fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), title="AppLoad")
                            ),
                            DeleteCoverInformation(
                                    id=fromString("2f050dc4-6a2e-46c3-8276-43098c011e68"),
                                    extension="png",
                                    item=Item(id= fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), title="Geek INC 124"),
                                    podcast=Podcast(id= fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), title="Geek Inc HD")
                            ),
                            DeleteCoverInformation(
                                    id=fromString("3ea0373e-7af6-4e15-b0fd-9ec4b10822ec"),
                                    extension="png",
                                    item=Item(id= fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), title="Appload 3"),
                                    podcast=Podcast(id= fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), title="AppLoad")
                            ),
                            DeleteCoverInformation(
                                    id=fromString("3f050dc4-6a2e-46c3-8276-43098c011e68"),
                                    extension="png",
                                    item=Item(id= fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), title="Geek INC 122"),
                                    podcast=Podcast(id= fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), title="Geek Inc HD")
                            ),
                            DeleteCoverInformation(
                                    id=fromString("4f050dc4-6a2e-46c3-8276-43098c011e68"),
                                    extension="png",
                                    item=Item(id= fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), title="Geek INC 126"),
                                    podcast=Podcast(id= fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), title="Geek Inc HD")
                            )
                    )
                    .verifyComplete()
        }


    }
    
    
    @TestConfiguration
    class LocalTestConfiguration {
        @Bean fun db(datasource: DataSource) = DataSourceDestination(datasource)
    }
}

private val formatter = DateTimeFormatterBuilder()
        .append(ISO_LOCAL_DATE)
        .appendLiteral(" ")
        .append(ISO_LOCAL_TIME)
        .toFormatter()

private val INSERT_COVER_DATA = sequenceOf(
        insertInto("COVER")
                .columns("ID", "URL", "WIDTH", "HEIGHT")
                .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                .values(fromString("1ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                .values(fromString("2ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                .values(fromString("3ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                .values(fromString("1f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                .values(fromString("2f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                .values(fromString("3f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                .values(fromString("4f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)

                .build(),

        insertInto("PODCAST")
                .columns("ID", "TITLE", "URL", "TYPE", "HAS_TO_BE_DELETED")
                .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), "AppLoad", null, "RSS", false)
                .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), "Geek Inc HD", "http://fake.url.com/rss", "YOUTUBE", true)
                .build(),

        insertInto("ITEM")
                .columns("ID", "TITLE", "URL", "FILE_NAME", "PODCAST_ID", "STATUS", "PUB_DATE", "DOWNLOAD_DATE", "CREATION_DATE", "NUMBER_OF_FAIL", "COVER_ID", "DESCRIPTION", "MIME_TYPE")

                .values(fromString("e3d41c71-37fb-4c23-a207-5fb362fa15bb"), "Appload 1", "http://fakeurl.com/appload.1.mp3", "appload.1.mp3", fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), Status.FINISH, now().minusDays(15).format(formatter), now().minusDays(15).format(formatter), now().minusDays(100).format(formatter), 0, fromString("1ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                .values(fromString("817a4626-6fd2-457e-8d27-69ea5acdc828"), "Appload 2", "http://fakeurl.com/appload.2.mp3", "appload.2.mp3", fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), null, now().minusDays(30).format(formatter), null, now().minusDays(100).format(formatter), 0, fromString("2ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")
                .values(fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"), "Appload 3", "http://fakeurl.com/appload.3.mp3", "appload.3.mp3", fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), Status.NOT_DOWNLOADED, now().format(formatter), null, now().minusDays(100).format(formatter), 0, fromString("3ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "desc", "audio/mp3")

                .values(fromString("b721a6b6-896a-48fc-b820-28aeafddbb53"), "Geek INC 123", "http://fakeurl.com/geekinc.123.mp3", "geekinc.123.mp3", fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.DELETED, now().minusYears(1).format(formatter), now().format(formatter), now().minusMonths(2).format(formatter), 0, fromString("1f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                .values(fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"), "Geek INC 124", "http://fakeurl.com/geekinc.124.mp3", "geekinc.124.mp3", fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FINISH, now().minusDays(15).format(formatter), now().minusDays(15).format(formatter), now().minusMonths(2).format(formatter), 0, fromString("2f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                .values(fromString("0a774611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 122", "http://fakeurl.com/geekinc.122.mp3", "geekinc.122.mp3", fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FAILED, now().minusDays(1).format(formatter), null, now().minusWeeks(2).format(formatter), 3, fromString("3f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                .values(fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"), "Geek INC 126", "http://fakeurl.com/geekinc.126.mp3", "geekinc.126.mp3", fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), Status.FAILED, now().minusDays(1).format(formatter), null, now().minusWeeks(1).format(formatter), 7, fromString("4f050dc4-6a2e-46c3-8276-43098c011e68"), "desc", "video/mp4")
                .build(),
        insertInto("TAG")
                .columns("ID", "NAME")
                .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Knowhere")
                .build(),
        insertInto("PODCAST_TAGS")
                .columns("PODCASTS_ID", "TAGS_ID")
                .values(fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                .values(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))
                .build()
)
