package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.tag.Tag
import com.ninja_squad.dbsetup.DbSetup
import com.ninja_squad.dbsetup.DbSetupTracker
import com.ninja_squad.dbsetup.Operations.insertInto
import com.ninja_squad.dbsetup.destination.DataSourceDestination
import com.ninja_squad.dbsetup.operation.CompositeOperation
import com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.DELETE_ALL
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.INSERT_ITEM_DATA
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.INSERT_PODCAST_DATA
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.INSERT_TAG_DATA
import lan.dk.podcastserver.repository.DatabaseConfigurationTest.formatter
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
import java.net.URI
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID.*
import javax.sql.DataSource
import com.github.davinkevin.podcastserver.podcast.PodcastRepositoryV2 as PodcastRepository

/**
 * Created by kevin on 2019-02-16
 */
@JooqTest
@Import(PodcastRepository::class)
class PodcastRepositoryV2Test {

    @Autowired lateinit var repository: PodcastRepository
    @Autowired lateinit var dataSource: DataSource
    @Autowired lateinit var query: DSLContext

    private val dbSetupTracker = DbSetupTracker()

    @Nested
    @DisplayName("Should find by id")
    inner class ShouldFindById {

        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_PODCAST_DATA)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Test
        fun `and return one matching element`() {
            /* Given */
            val id = fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d")

            /* When */
            StepVerifier.create(repository.findById(id))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isEqualTo(id)
                        assertThat(it.title).isEqualTo("Geek Inc HD")
                        assertThat(it.url).isEqualTo("http://fake.url.com/geekinc.rss")
                        assertThat(it.hasToBeDeleted).isEqualTo(true)
                        assertThat(it.type).isEqualTo("Youtube")
                        assertThat(it.tags).containsOnly(
                                Tag(fromString("df801a7a-5630-4442-8b83-0cb36ae94981"),"Geek"),
                                Tag(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"),"Studio Renegade")
                        )
                        assertThat(it.cover.id).isEqualTo(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"))
                        assertThat(it.cover.url).isEqualTo(URI("http://fake.url.com/appload/cover.png"))
                        assertThat(it.cover.width).isEqualTo(100)
                        assertThat(it.cover.height).isEqualTo(100)
                    }
                    .verifyComplete()
        }

        @Test
        fun `and don't return any element`() {
            /* Given */
            val id = fromString("ef85dcd3-758c-573f-a8fc-b82104762d9d")

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

        private val currentNow = ZonedDateTime.of(2019, 3, 15, 11, 12, 2, 0, ZoneId.systemDefault())!!

        private val multipleTypeOfPodcasts = CompositeOperation.sequenceOf(
                insertInto("COVER")
                        .columns("ID", "URL", "WIDTH", "HEIGHT")
                        .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)
                        .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                        .values(fromString("c46d93af-4461-4299-a42a-dd28d3f376e9"), "http://fake.url.com/notags/cover.png", 100, 100)
                        .build(),
                insertInto("PODCAST")
                        .columns("ID", "TITLE", "URL", "COVER_ID", "HAS_TO_BE_DELETED", "TYPE", "LAST_UPDATE")
                        .values(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), "AppLoad", "http://fake.url.com/appload.rss", fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), false, "RSS", currentNow.minusDays(15).format(formatter))
                        .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD", "http://fake.url.com/geekinc.rss", fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), true, "Youtube", currentNow.minusDays(30).format(formatter))
                        .values(fromString("0311361c-cc97-48ab-b02a-0bd19eec8a45"), "Without tags", "http://fake.url.com/notags.rss", fromString("c46d93af-4461-4299-a42a-dd28d3f376e9"), true, "Youtube", currentNow.minusDays(45).format(formatter))
                        .build(),
                insertInto("TAG")
                        .columns("ID", "NAME")
                        .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "French Spin")
                        .values(fromString("df801a7a-5630-4442-8b83-0cb36ae94981"), "Geek")
                        .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "Studio Renegade")
                        .build(),
                insertInto("PODCAST_TAGS")
                        .columns("PODCASTS_ID", "TAGS_ID")
                        .values(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                        .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), fromString("df801a7a-5630-4442-8b83-0cb36ae94981"))
                        .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))
                        .build()
        )


        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, multipleTypeOfPodcasts)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Test
        fun `with different cardinality with tags`() {
            /* Given */
            /* When */
            StepVerifier.create(repository.findAll())
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isEqualTo(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"))
                        assertThat(it.title).isEqualTo("AppLoad")
                        assertThat(it.url).isEqualTo("http://fake.url.com/appload.rss")
                        assertThat(it.hasToBeDeleted).isFalse()
                        assertThat(it.type).isEqualTo("RSS")
                        assertThat(it.lastUpdate).isBetween(currentNow.minusDays(16).toOffsetDateTime(), currentNow.minusDays(14).toOffsetDateTime())
                        assertThat(it.tags).hasSize(1)
                        assertThat(it.tags.map(Tag::name)).containsExactly("French Spin")
                        assertThat(it.cover).isEqualTo(CoverForPodcast(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), URI("http://fake.url.com/appload/cover.png"), 100, 100))
                    }
                    .assertNext {
                        assertThat(it.id).isEqualTo(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"))
                        assertThat(it.title).isEqualTo("Geek Inc HD")
                        assertThat(it.url).isEqualTo("http://fake.url.com/geekinc.rss")
                        assertThat(it.hasToBeDeleted).isTrue()
                        assertThat(it.type).isEqualTo("Youtube")
                        assertThat(it.lastUpdate).isBetween(currentNow.minusDays(31).toOffsetDateTime(), currentNow.minusDays(29).toOffsetDateTime())
                        assertThat(it.tags).hasSize(2)
                        assertThat(it.tags.map(Tag::name)).containsExactly("Geek", "Studio Renegade")
                        assertThat(it.cover).isEqualTo(CoverForPodcast(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), URI("http://fake.url.com/geekinc/cover.png"), 100, 100))
                    }
                    .assertNext {
                        assertThat(it.id).isEqualTo(fromString("0311361c-cc97-48ab-b02a-0bd19eec8a45"))
                        assertThat(it.title).isEqualTo("Without tags")
                        assertThat(it.url).isEqualTo("http://fake.url.com/notags.rss")
                        assertThat(it.hasToBeDeleted).isTrue()
                        assertThat(it.type).isEqualTo("Youtube")
                        assertThat(it.lastUpdate).isBetween(currentNow.minusDays(46).toOffsetDateTime(), currentNow.minusDays(44).toOffsetDateTime())
                        assertThat(it.tags).hasSize(0)
                        assertThat(it.cover).isEqualTo(CoverForPodcast(fromString("c46d93af-4461-4299-a42a-dd28d3f376e9"), URI("http://fake.url.com/notags/cover.png"), 100, 100))
                    }
                    .verifyComplete()
        }

    }

    @Nested
    @DisplayName("should find stats")
    inner class ShouldFindStats {

        @Nested
        @DisplayName("by podcast")
        inner class ByPodcast {

            @BeforeEach
            fun prepare() {
                val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
                val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)
                dbSetupTracker.launchIfNecessary(dbSetup)
            }

            @Test
            fun `by pubDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByPodcastIdAndPubDate(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), 13))
                        /* Then */
                        .expectSubscription()
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusDays(1), 2))
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1))
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusYears(1), 1))
                        .verifyComplete()
            }

            @Test
            fun `by downloadDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByPodcastIdAndDownloadDate(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), 13))
                        /* Then */
                        .expectSubscription()
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now(), 1))
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1))
                        .verifyComplete()
            }

            @Test
            fun `by creationDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByPodcastIdAndCreationDate(fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"), 13))
                        /* Then */
                        .expectSubscription()
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusWeeks(1), 1))
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusWeeks(2), 1))
                        .expectNext(NumberOfItemByDateWrapper(LocalDate.now().minusMonths(2), 2))
                        .verifyComplete()
            }

        }

        @Nested
        @DisplayName("globally")
        inner class Globally {

            @BeforeEach
            fun prepare() {
                val operation = sequenceOf(DELETE_ALL, INSERT_ITEM_DATA)
                val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)
                dbSetupTracker.launchIfNecessary(dbSetup)
            }

            @Test
            fun `by pubDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByTypeAndPubDate(13))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.type).isEqualTo("RSS")
                            assertThat(it.values).contains(
                                    NumberOfItemByDateWrapper(LocalDate.now(), 1),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(30), 1)
                            )
                        }
                        .assertNext {
                            assertThat(it.type).isEqualTo("YOUTUBE")
                            assertThat(it.values).contains(
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(1), 2),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusYears(1), 1)
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `by downloadDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByTypeAndDownloadDate(13))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.type).isEqualTo("YOUTUBE")
                            assertThat(it.values).contains(
                                    NumberOfItemByDateWrapper(LocalDate.now(), 1),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1)
                            )
                        }
                        .assertNext {
                            assertThat(it.type).isEqualTo("RSS")
                            assertThat(it.values).contains(
                                    NumberOfItemByDateWrapper(LocalDate.now().minusDays(15), 1)
                            )
                        }
                        .verifyComplete()
            }

            @Test
            fun `by creationDate`() {
                /* Given */
                /* When */
                StepVerifier.create(repository.findStatByTypeAndCreationDate(13))
                        /* Then */
                        .expectSubscription()
                        .assertNext {
                            assertThat(it.type).isEqualTo("YOUTUBE")
                            assertThat(it.values).contains(
                                    NumberOfItemByDateWrapper(LocalDate.now().minusWeeks(1), 1),
                                    NumberOfItemByDateWrapper(LocalDate.now().minusMonths(2), 1)
                            )
                        }
                        .verifyComplete()
            }


        }


    }

    @Nested
    @DisplayName("should save")
    inner class ShouldSave {

        private val podcastCover = insertInto("COVER")
                .columns("ID", "URL", "WIDTH", "HEIGHT")
                .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/a-podcast-to-save/cover.png", 100, 100)
                .build()!!

        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, INSERT_TAG_DATA, podcastCover)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Nested
        @DisplayName("a new podcast")
        inner class ANewPodcast {

            private val tag1 = Tag(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "Foo")
            private val tag2 = Tag(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "bAr")
            private val tag3 = Tag(fromString("ad109389-9568-4bdb-ae61-6f26bf6ffdf6"), "Another Bar")

            @Test
            fun `with many tags`() {
                /* Given */

                /* When */
                StepVerifier.create(repository.save(
                        title = "a podcast",
                        url = "http://foo.bar.com/feed.rss",
                        hasToBeDeleted = true,
                        type = "Rss",
                        tags = listOf(tag1, tag2, tag3),
                        cover = Cover(
                                id = fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"),
                                url = URI("http://fake.url.com/a-podcast-to-save/cover.png"),
                                width = 100, height = 100
                        )
                ))
                        .expectSubscription()
                        /* Then */
                        .assertNext {
                            assertThat(it.title).isEqualTo("a podcast")
                            assertThat(it.url).isEqualTo("http://foo.bar.com/feed.rss")
                            assertThat(it.hasToBeDeleted).isTrue()
                            assertThat(it.type).isEqualTo("Rss")
                            assertThat(it.tags.map(Tag::id)).containsExactlyInAnyOrder(tag1.id, tag2.id, tag3.id)
                            assertThat(it.cover.id).isEqualTo(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"))
                        }
                        .verifyComplete()
            }

            @Test
            fun `with no tag`() {
                /* Given */

                /* When */
                StepVerifier.create(repository.save(
                        title = "a podcast",
                        url = "http://foo.bar.com/feed.rss",
                        hasToBeDeleted = true,
                        type = "Rss",
                        tags = listOf(),
                        cover = Cover(
                                id = fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"),
                                url = URI("http://fake.url.com/a-podcast-to-save/cover.png"),
                                width = 100, height = 100
                        )
                ))
                        .expectSubscription()
                        /* Then */
                        .assertNext {
                            assertThat(it.title).isEqualTo("a podcast")
                            assertThat(it.url).isEqualTo("http://foo.bar.com/feed.rss")
                            assertThat(it.hasToBeDeleted).isTrue()
                            assertThat(it.type).isEqualTo("Rss")
                            assertThat(it.tags.map(Tag::id)).isEmpty()
                            assertThat(it.cover.id).isEqualTo(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"))
                        }
                        .verifyComplete()
            }


        }

    }

    @Nested
    @DisplayName("should update")
    inner class ShouldUpdate {

        private val podcastCovers = insertInto("COVER")
                .columns("ID", "URL", "WIDTH", "HEIGHT")
                .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/a-podcast-to-update/cover_1.png", 100, 100)
                .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/a-podcast-to-update/cover_2.png", 200, 200)
                .values(fromString("8ea0373e-7af8-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/a-podcast-to-update/cover_3.png", 300, 300)
                .values(fromString("8ea0373e-7af9-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/a-podcast-to-update/cover_4.png", 400, 400)
                .build()!!

        private val podcastToUpdate = insertInto("PODCAST")
                .columns("ID", "TITLE", "URL", "COVER_ID", "HAS_TO_BE_DELETED", "TYPE")
                .values(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), "podcast without tags", "http://fake.url.com/appload.rss", fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), false, "RSS")
                .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD", "http://fake.url.com/geekinc.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822ec"), true, "RSS")
                .build()!!

        private val tagsInDb = insertInto("TAG")
                .columns("ID", "NAME")
                .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "Foo")
                .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "bAr")
                .values(fromString("ad109389-9568-4bdb-ae61-6f26bf6ffdf6"), "Another Bar")
                .build()!!

        private val podcastTag = insertInto("PODCAST_TAGS")
                .columns("PODCASTS_ID", "TAGS_ID")
                .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), fromString("eb355a23-e030-4966-b75a-b70881a8bd08"))
                .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"))
                .build()!!


        @BeforeEach
        fun prepare() {
            val operation = sequenceOf(DELETE_ALL, tagsInDb, podcastCovers, podcastToUpdate, podcastTag)
            val dbSetup = DbSetup(DataSourceDestination(dataSource), operation)

            dbSetupTracker.launchIfNecessary(dbSetup)
        }

        @Test
        fun `the title`() {
            /* Given */
            val id = fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")

            /* When */
            StepVerifier.create(repository.update(
                    id = id,
                    title = "new title",
                    url = "http://fake.url.com/appload.rss",
                    hasToBeDeleted = false,
                    tags = listOf(),
                    cover = Cover(id = fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), url = URI("http://foo.bar.com/a/cover.png"), height = 100, width = 100)
            ))
                    /* Then */
                    .expectSubscription()
                    .expectNext(Podcast(
                            id = id,
                            title = "new title",
                            description = null,
                            signature = null,
                            url = "http://fake.url.com/appload.rss",
                            hasToBeDeleted = false,
                            lastUpdate = null,
                            type = "RSS",
                            tags = listOf(),
                            cover = CoverForPodcast(id = fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), url = URI("http://fake.url.com/a-podcast-to-update/cover_1.png"), height = 100, width = 100)
                    ))
                    .verifyComplete()
        }

        @Test
        fun `the url`() {
            /* Given */
            val id = fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")

            /* When */
            StepVerifier.create(repository.update(
                    id = id,
                    title = "new title",
                    url = "http://fake.url.com/new-url.rss",
                    hasToBeDeleted = false,
                    tags = listOf(),
                    cover = Cover(id = fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), url = URI("http://foo.bar.com/a/cover.png"), height = 100, width = 100)
            ))
                    /* Then */
                    .expectSubscription()
                    .expectNext(Podcast(
                            id = id,
                            title = "new title",
                            description = null,
                            signature = null,
                            url = "http://fake.url.com/new-url.rss",
                            hasToBeDeleted = false,
                            lastUpdate = null,
                            type = "RSS",
                            tags = listOf(),
                            cover = CoverForPodcast(id = fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), url = URI("http://fake.url.com/a-podcast-to-update/cover_1.png"), height = 100, width = 100)
                    ))
                    .verifyComplete()
        }

        @Test
        fun `has to be deleted`() {
            /* Given */
            val id = fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")

            /* When */
            StepVerifier.create(repository.update(
                    id = id,
                    title = "new title",
                    url = "http://fake.url.com/appload.rss",
                    hasToBeDeleted = true,
                    tags = listOf(),
                    cover = Cover(id = fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), url = URI("http://foo.bar.com/a/cover.png"), height = 100, width = 100)
            ))
                    /* Then */
                    .expectSubscription()
                    .expectNext(Podcast(
                            id = id,
                            title = "new title",
                            description = null,
                            signature = null,
                            url = "http://fake.url.com/appload.rss",
                            hasToBeDeleted = true,
                            lastUpdate = null,
                            type = "RSS",
                            tags = listOf(),
                            cover = CoverForPodcast(id = fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), url = URI("http://fake.url.com/a-podcast-to-update/cover_1.png"), height = 100, width = 100)
                    ))
                    .verifyComplete()
        }

        @Test
        fun `the cover id`() {
            /* Given */
            val id = fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")

            /* When */
            StepVerifier.create(repository.update(
                    id = id,
                    title = "new title",
                    url = "http://fake.url.com/appload.rss",
                    hasToBeDeleted = true,
                    tags = listOf(),
                    cover = Cover(id = fromString("8ea0373e-7af9-4e15-b0fd-9ec4b10822ec"), url = URI("http://foo.bar.com/a/cover.png"), height = 100, width = 100)
            ))
                    /* Then */
                    .expectSubscription()
                    .expectNext(Podcast(
                            id = id,
                            title = "new title",
                            description = null,
                            signature = null,
                            url = "http://fake.url.com/appload.rss",
                            hasToBeDeleted = true,
                            lastUpdate = null,
                            type = "RSS",
                            tags = listOf(),
                            cover = CoverForPodcast(id = fromString("8ea0373e-7af9-4e15-b0fd-9ec4b10822ec"), url = URI("http://fake.url.com/a-podcast-to-update/cover_4.png"), height = 400, width = 400)
                    ))
                    .verifyComplete()
        }

        @Test
        fun `to add Foo, bAr and another_Bar tags`() {
            /* Given */
            val id = fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d")

            val tag1 = Tag(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "Foo")
            val tag2 = Tag(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "bAr")
            val tag3 = Tag(fromString("ad109389-9568-4bdb-ae61-6f26bf6ffdf6"), "Another Bar")

            /* When */
            StepVerifier.create(repository.update(
                    id = id,
                    title = "Geek Inc HD",
                    url = "http://fake.url.com/geekinc.rss",
                    hasToBeDeleted = true,
                    tags = listOf(tag1, tag2, tag3),
                    cover = Cover(id = fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822ec"), url = URI("http://foo.bar.com/a/cover.png"), height = 100, width = 100)
            ))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isEqualTo(id)
                        assertThat(it.title).isEqualTo("Geek Inc HD")
                        assertThat(it.url).isEqualTo("http://fake.url.com/geekinc.rss")
                        assertThat(it.hasToBeDeleted).isEqualTo(true)
                        assertThat(it.type).isEqualTo("RSS")
                        assertThat(it.tags).containsExactlyInAnyOrder(tag1, tag2, tag3)
                        assertThat(it.cover).isEqualTo(CoverForPodcast(id = fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822ec"), url = URI("http://fake.url.com/a-podcast-to-update/cover_2.png"), height = 200, width = 200))
                    }
                    .verifyComplete()
        }

        @Test
        fun `to remove all tags`() {
            /* Given */
            val id = fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d")

            /* When */
            StepVerifier.create(repository.update(
                    id = id,
                    title = "Geek Inc HD",
                    url = "http://fake.url.com/geekinc.rss",
                    hasToBeDeleted = true,
                    tags = listOf(),
                    cover = Cover(id = fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822ec"), url = URI("http://foo.bar.com/a/cover.png"), height = 100, width = 100)
            ))
                    /* Then */
                    .expectSubscription()
                    .assertNext {
                        assertThat(it.id).isEqualTo(id)
                        assertThat(it.title).isEqualTo("Geek Inc HD")
                        assertThat(it.url).isEqualTo("http://fake.url.com/geekinc.rss")
                        assertThat(it.hasToBeDeleted).isEqualTo(true)
                        assertThat(it.type).isEqualTo("RSS")
                        assertThat(it.tags).isEmpty()
                        assertThat(it.cover).isEqualTo(CoverForPodcast(id = fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822ec"), url = URI("http://fake.url.com/a-podcast-to-update/cover_2.png"), height = 200, width = 200))
                    }
                    .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should delete")
    inner class ShouldDelete {

        @Test
        fun `a podcast with cover`() {
            /* Given */
            val podcastCovers = insertInto("COVER")
                    .columns("ID", "URL", "WIDTH", "HEIGHT")
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e1"), "http://fake.url.com/a-podcast-to-update/cover_1.png", 100, 100)
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2"), "http://fake.url.com/a-podcast-to-update/cover_2.png", 100, 100)
                    .build()!!

            val podcastInDb = insertInto("PODCAST")
                    .columns("ID", "TITLE", "URL", "COVER_ID", "HAS_TO_BE_DELETED", "TYPE")
                    .values(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), "Appload",      "http://fake.url.com/appload.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e1"), false, "RSS")
                    .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD",  "http://fake.url.com/geekinc.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2"), true,  "RSS")
                    .build()!!

            DbSetup(DataSourceDestination(dataSource), sequenceOf(DELETE_ALL, podcastCovers, podcastInDb)).launch()

            /* When */
            StepVerifier.create(repository.deleteById(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            assertThat(query.selectFrom(COVER).fetch().map { it[COVER.ID] }).containsOnly(
                    fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2")
            )
            assertThat(query.selectFrom(PODCAST).fetch().map { it[PODCAST.ID] }).containsOnly(
                    fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d")
            )
        }

        @Test
        fun `a podcast with cover and should be deleted`() {
            /* Given */
            val podcastCovers = insertInto("COVER")
                    .columns("ID", "URL", "WIDTH", "HEIGHT")
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e1"), "http://fake.url.com/a-podcast-to-update/cover_1.png", 100, 100)
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2"), "http://fake.url.com/a-podcast-to-update/cover_2.png", 100, 100)
                    .build()!!

            val podcastInDb = insertInto("PODCAST")
                    .columns("ID", "TITLE", "URL", "COVER_ID", "HAS_TO_BE_DELETED", "TYPE")
                    .values(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), "Appload",      "http://fake.url.com/appload.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e1"), false, "RSS")
                    .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD",  "http://fake.url.com/geekinc.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2"), true,  "RSS")
                    .build()!!

            DbSetup(DataSourceDestination(dataSource), sequenceOf(DELETE_ALL, podcastCovers, podcastInDb)).launch()

            /* When */
            StepVerifier.create(repository.deleteById(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d")))
                    /* Then */
                    .expectSubscription()
                    .expectNext(DeletePodcastInformation(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD"))
                    .verifyComplete()
        }

        @Test
        fun `a podcast with cover and tags`() {
            /* Given */
            val podcastCovers = insertInto("COVER")
                    .columns("ID", "URL", "WIDTH", "HEIGHT")
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e1"), "http://fake.url.com/a-podcast-to-update/cover_1.png", 100, 100)
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2"), "http://fake.url.com/a-podcast-to-update/cover_2.png", 100, 100)
                    .build()!!

            val podcastInDb = insertInto("PODCAST")
                    .columns("ID", "TITLE", "URL", "COVER_ID", "HAS_TO_BE_DELETED", "TYPE")
                    .values(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), "Appload",      "http://fake.url.com/appload.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e1"), false, "RSS")
                    .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD",  "http://fake.url.com/geekinc.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2"), true,  "RSS")
                    .build()!!

            val tagsInDb = sequenceOf(
                    insertInto("TAG")
                            .columns("ID", "NAME")
                            .values(fromString("1c526048-f240-4db9-9526-6cc037fdc851"), "First")
                            .values(fromString("2c526048-f240-4db9-9526-6cc037fdc851"), "Second")
                            .values(fromString("3c526048-f240-4db9-9526-6cc037fdc851"), "Third")
                            .build(),
                    insertInto("PODCAST_TAGS")
                            .columns("PODCASTS_ID", "TAGS_ID")
                            .values(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), fromString("1c526048-f240-4db9-9526-6cc037fdc851"))
                            .values(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), fromString("2c526048-f240-4db9-9526-6cc037fdc851"))
                            .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), fromString("3c526048-f240-4db9-9526-6cc037fdc851"))
                            .build()
            )

            DbSetup(DataSourceDestination(dataSource), sequenceOf(DELETE_ALL, podcastCovers, podcastInDb, tagsInDb)).launch()
            /* When */
            StepVerifier.create(repository.deleteById(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            assertThat(query.selectFrom(PODCAST_TAGS).fetch().map { it[PODCAST_TAGS.PODCASTS_ID] to it[PODCAST_TAGS.TAGS_ID]  }).containsOnly(
                    fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d") to fromString("3c526048-f240-4db9-9526-6cc037fdc851")
            )
        }

        @Test
        fun `a podcast with cover and items`() {
            /* Given */
            val podcastCovers = insertInto("COVER")
                    .columns("ID", "URL", "WIDTH", "HEIGHT")
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e1"), "http://fake.url.com/a-podcast-to-update/cover_1.png", 100, 100)
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2"), "http://fake.url.com/a-podcast-to-update/cover_2.png", 100, 100)
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e3"), "http://fake.url.com/a-podcast-to-update/cover_3.png", 100, 100)
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e4"), "http://fake.url.com/a-podcast-to-update/cover_4.png", 100, 100)
                    .build()!!

            val podcastInDb = insertInto("PODCAST")
                    .columns("ID", "TITLE", "URL", "COVER_ID", "HAS_TO_BE_DELETED", "TYPE")
                    .values(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), "Appload",      "http://fake.url.com/appload.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e1"), false, "RSS")
                    .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD",  "http://fake.url.com/geekinc.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2"), true,  "RSS")
                    .build()!!

            val itemsInDb = sequenceOf(insertInto("ITEM")
                    .columns("ID", "TITLE", "URL", "PODCAST_ID", "COVER_ID")
                    .values(fromString("1b83a383-25ec-4aeb-8e82-f317449da37b"), "Item 1", "http://fakeurl.com/item.1.mp3", fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e3"))
                    .values(fromString("2b83a383-25ec-4aeb-8e82-f317449da37b"), "Item 2", "http://fakeurl.com/item.2.mp3", fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e4"))
                    .build()
            )

            DbSetup(DataSourceDestination(dataSource), sequenceOf(DELETE_ALL, podcastCovers, podcastInDb, itemsInDb)).launch()

            /* When */
            StepVerifier.create(repository.deleteById(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            assertThat(query.selectFrom(ITEM).fetch().map { it[ITEM.ID] }).containsOnly(
                    fromString("2b83a383-25ec-4aeb-8e82-f317449da37b")
            )
        }

        @Test
        fun `a podcast with cover and items in playlist`() {
            /* Given */
            val podcastCovers = insertInto("COVER")
                    .columns("ID", "URL", "WIDTH", "HEIGHT")
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e1"), "http://fake.url.com/a-podcast-to-update/cover_1.png", 100, 100)
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2"), "http://fake.url.com/a-podcast-to-update/cover_2.png", 100, 100)
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e3"), "http://fake.url.com/a-podcast-to-update/cover_3.png", 100, 100)
                    .values(fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e4"), "http://fake.url.com/a-podcast-to-update/cover_4.png", 100, 100)
                    .build()!!

            val podcastInDb = insertInto("PODCAST")
                    .columns("ID", "TITLE", "URL", "COVER_ID", "HAS_TO_BE_DELETED", "TYPE")
                    .values(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), "Appload",      "http://fake.url.com/appload.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e1"), false, "RSS")
                    .values(fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), "Geek Inc HD",  "http://fake.url.com/geekinc.rss", fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e2"), true,  "RSS")
                    .build()!!

            val itemsInDb = sequenceOf(insertInto("ITEM")
                    .columns("ID", "TITLE", "URL", "PODCAST_ID", "COVER_ID")
                    .values(fromString("1b83a383-25ec-4aeb-8e82-f317449da37b"), "Item 1", "http://fakeurl.com/item.1.mp3", fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82"), fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e3"))
                    .values(fromString("2b83a383-25ec-4aeb-8e82-f317449da37b"), "Item 2", "http://fakeurl.com/item.2.mp3", fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d"), fromString("8ea0373e-7af7-4e15-b0fd-9ec4b10822e4"))
                    .build()
            )

            val playlistInDb = sequenceOf(
                    insertInto("WATCH_LIST")
                            .columns("ID", "NAME")
                            .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist")
                            .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind")
                            .build(),
                    insertInto("WATCH_LIST_ITEMS")
                            .columns("WATCH_LISTS_ID", "ITEMS_ID")
                            .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("1b83a383-25ec-4aeb-8e82-f317449da37b"))
                            .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("1b83a383-25ec-4aeb-8e82-f317449da37b"))
                            .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("2b83a383-25ec-4aeb-8e82-f317449da37b"))
                            .build()
            )

            DbSetup(DataSourceDestination(dataSource), sequenceOf(DELETE_ALL, podcastCovers, podcastInDb, itemsInDb, playlistInDb)).launch()

            /* When */
            StepVerifier.create(repository.deleteById(fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            assertThat(query.selectFrom(WATCH_LIST_ITEMS).fetch().map { it[WATCH_LIST_ITEMS.WATCH_LISTS_ID] to it[WATCH_LIST_ITEMS.ITEMS_ID] }).containsOnly(
                    fromString("dc024a30-bd02-11e5-a837-0800200c9a66") to fromString("2b83a383-25ec-4aeb-8e82-f317449da37b")
            )
        }

    }
}
