package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.database.Tables.*
import com.github.davinkevin.podcastserver.database.enums.ItemStatus
import com.github.davinkevin.podcastserver.extension.assertthat.assertAll
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID.fromString
import kotlin.io.path.Path

/**
 * Created by kevin on 2019-07-06
 */
@JooqTest
@Import(PlaylistRepository::class)
class PlaylistRepositoryTest(
    @Autowired val query: DSLContext,
    @Autowired val repository: PlaylistRepository
) {

    private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)

    @BeforeEach
    fun beforeEach() {
        query.batch(
            truncate(PODCAST_TAGS).cascade(),
            truncate(PODCAST).cascade(),
            truncate(COVER).cascade(),
            truncate(TAG).cascade(),
            truncate(PLAYLIST).cascade(),
            truncate(PLAYLIST_ITEMS).cascade(),

            insertInto(COVER)
                .columns(COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                .values(fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"), "http://fake.url.com/geekinc/cover.png", 100, 100)
                .values(fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"), "http://fake.url.com/appload/cover.png", 100, 100)

                .values(fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"), "http://fake.url.com/playlist-1/cover.png", 100, 100)
                .values(fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"), "http://fake.url.com/playlist-2/cover.png", 100, 100)
                .values(fromString("71775935-59dc-4e8f-9706-15877b27eec4"), "http://fake.url.com/playlist-3/cover.png", 100, 100),

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
                .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist",   fromString("f496ffad-1bb6-44df-bbd1-bca9a2891918"))
                .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind", fromString("51a51b22-4a6f-45df-8905-f5e691eafee4"))
                .values(fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"), "empty playlist",    fromString("71775935-59dc-4e8f-9706-15877b27eec4")),

            insertInto(PLAYLIST_ITEMS)
                .columns(PLAYLIST_ITEMS.PLAYLISTS_ID, PLAYLIST_ITEMS.ITEMS_ID)
                .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"))
                .values(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"))
                .values(fromString("24248480-bd04-11e5-a837-0800200c9a66"), fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"))

        ).execute()
    }

    @Nested
    @DisplayName("should find all")
    inner class ShouldFindAll {
        @Test
        fun `with watch lists in results`() {
            /* Given */
            /* When */
            val playlists = repository.findAll()
            /* Then */
            assertThat(playlists).containsExactly(
                Playlist(fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind"),
                Playlist(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist"),
                Playlist(fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"), "empty playlist"),
            )
        }
    }

    @Nested
    @DisplayName("should find by id")
    inner class ShouldFindById {

        @Test
        fun `and returns null because the playlist doesn't exist`() {
            /* Given */
            val id = fromString("d2c0d935-10c7-47a7-aa70-83cbdf1f93ca")

            /* When */
            val playlist = repository.findById(id)

            /* Then */
            assertThat(playlist).isNull()
        }

        @Test
        fun `with no item`() {
            /* Given */
            val id = fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea")

            /* When */
            val playlist = repository.findById(id)!!

            /* Then */
            assertAll {
                assertThat(playlist.name).isEqualTo("empty playlist")
                assertThat(playlist.id).isEqualTo(id)
                assertThat(playlist.items).isEmpty()
            }
        }

        @Test
        fun `with 1 item`() {
            /* Given */
            val id = fromString("24248480-bd04-11e5-a837-0800200c9a66")

            /* When */
            val playlist = repository.findById(id)!!

            /* Then */
            assertAll {
                assertThat(playlist.name).isEqualTo("Conférence Rewind")
                assertThat(playlist.id).isEqualTo(id)
                assertThat(playlist.items)
                    .containsOnly(
                        PlaylistWithItems. Item(
                            id = fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"),
                            title = "Geek INC 124",
                            fileName = Path("geekinc.124.mp3"),
                            description = "desc",
                            mimeType = "video/mp4",
                            length = null,
                            pubDate = fixedDate.minusDays(15),
                            podcast = PlaylistWithItems.Item.Podcast(
                                id = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                                title = "Geek Inc HD"
                            ),
                            cover = PlaylistWithItems.Item.Cover(
                                id = fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"),
                                width = 100,
                                height = 100,
                                url = URI("http://fake.url.com/geekinc/cover.png")))
                    )
            }
        }

        @Test
        fun `with 2 items`() {
            /* Given */
            val id = fromString("dc024a30-bd02-11e5-a837-0800200c9a66")
            /* When */
            val playlist = repository.findById(id)!!

            /* Then */
            assertAll {
                assertThat(playlist.name).isEqualTo("Humour Playlist")
                assertThat(playlist.id).isEqualTo(id)
                assertThat(playlist.items).containsOnly(
                    PlaylistWithItems.Item(
                        id = fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd"),
                        title = "Appload 3",
                        fileName = Path("appload.3.mp3"),
                        description = "desc",
                        mimeType = "audio/mp3",
                        length = null,
                        pubDate = fixedDate,
                        podcast = PlaylistWithItems.Item.Podcast(
                            id = fromString("e9c89e7f-7a8a-43ad-8425-ba2dbad2c561"),
                            title = "AppLoad"
                        ),
                        cover = PlaylistWithItems.Item.Cover(
                            id = fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec"),
                            width = 100,
                            height = 100,
                            url = URI("http://fake.url.com/appload/cover.png")
                        )
                    ),
                    PlaylistWithItems. Item(
                        id = fromString("0a774611-c857-44df-b7e0-5e5af31f7b56"),
                        title = "Geek INC 124",
                        fileName = Path("geekinc.124.mp3"),
                        description = "desc",
                        mimeType = "video/mp4",
                        length = null,
                        pubDate = fixedDate.minusDays(15),
                        podcast = PlaylistWithItems.Item.Podcast(
                            id = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                            title = "Geek Inc HD"
                        ),
                        cover = PlaylistWithItems.Item.Cover(
                            id = fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"),
                            width = 100,
                            height = 100,
                            url = URI("http://fake.url.com/geekinc/cover.png")))
                )
            }
        }
    }

    @Nested
    @DisplayName("should save")
    inner class ShouldSave {

        @Test
        fun `with a name`() {
            /* Given */
            val saveRequest = PlaylistRepository.SaveRequest(
                name = "foo",
                cover = PlaylistRepository.SaveRequest.Cover(
                    height = 100,
                    width = 200,
                    url = URI("https://fake.url.com/cover.png")
                )
            )

            /* When */
            val playlist = repository.save(saveRequest)

            /* Then */
            assertAll {
                assertThat(playlist.id).isNotNull()
                assertThat(playlist.name).isEqualTo("foo")
                assertThat(playlist.items).isEmpty()
            }

            val numberOfPlaylist = query.selectCount().from(PLAYLIST).fetchOne(count())
            assertThat(numberOfPlaylist).isEqualTo(4)
        }

        @Test
        fun `with an already existing name`() {
            /* Given */
            val saveRequest = PlaylistRepository.SaveRequest(
                name = "Humour Playlist",
                cover = PlaylistRepository.SaveRequest.Cover(
                    height = 100,
                    width = 200,
                    url = URI("https://fake.url.com/cover.png")
                )
            )

            /* When */
            val playlist = repository.save(saveRequest)

            /* Then */
            assertAll {
                assertThat(playlist.id).isNotNull()
                assertThat(playlist.name).isEqualTo("Humour Playlist")
                assertThat(playlist.items).hasSize(2)
            }
            val numberOfPlaylist = query.selectCount().from(PLAYLIST).fetchOne(count())
            assertThat(numberOfPlaylist).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("should delete")
    inner class ShouldDelete {
        @Test
        fun `by id with no items`() {
            /* Given */

            /* When */
            repository.deleteById(fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea"))

            /* Then */
            assertThat(query.selectCount().from(PLAYLIST).fetchOne(count())).isEqualTo(2)
            assertThat(query.selectCount().from(PLAYLIST_ITEMS).fetchOne(count())).isEqualTo(3)
        }

        @Test
        fun `by id with items`() {
            /* Given */

            /* When */
            repository.deleteById(fromString("dc024a30-bd02-11e5-a837-0800200c9a66"))

            /* Then */
            assertThat(query.selectCount().from(PLAYLIST).fetchOne(count())).isEqualTo(2)
            assertThat(query.selectCount().from(PLAYLIST_ITEMS).fetchOne(count())).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("should add")
    inner class ShouldAdd {

        @Test
        fun `item to playlist`() {
            /* Given */
            val playlistId = fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea")
            val itemId = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")

            /* When */
            val playlist = repository.addToPlaylist(playlistId, itemId)

            /* Then */
            assertThat(playlist.items).containsOnly(
                PlaylistWithItems. Item(
                    id = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56"),
                    title = "Geek INC 126",
                    fileName = Path("geekinc.126.mp3"),
                    description = "desc",
                    mimeType = "video/mp4",
                    length = null,
                    pubDate = fixedDate.minusDays(1),
                    podcast = PlaylistWithItems.Item.Podcast(
                        id = fromString("67b56578-454b-40a5-8d55-5fe1a14673e8"),
                        title = "Geek Inc HD"
                    ),
                    cover = PlaylistWithItems.Item.Cover(
                        id = fromString("9f050dc4-6a2e-46c3-8276-43098c011e68"),
                        width = 100,
                        height = 100,
                        url = URI("http://fake.url.com/geekinc/cover.png")))
            )

        }

        @Test
        fun `item to playlist which already exist`() {
            /* Given */
            val playlistId = fromString("dc024a30-bd02-11e5-a837-0800200c9a66")
            val itemId = fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")

            /* When */
            val playlist = repository.addToPlaylist(playlistId, itemId)

            /* Then */
            assertThat(playlist.items).hasSize(2)
        }
    }

    @Nested
    @DisplayName("should remove")
    inner class ShouldRemove {

        @Test
        fun `item from playlist`() {
            /* Given */
            val playlistId = fromString("dc024a30-bd02-11e5-a837-0800200c9a66")
            val itemId = fromString("43fb990f-0b5e-413f-920c-6de217f9ecdd")

            /* When */
            val playlist = repository.removeFromPlaylist(playlistId, itemId)

            /* Then */
            assertThat(playlist.items).hasSize(1)
        }

        @Test
        fun `item from playlist which wasn't linked`() {
            /* Given */
            val playlistId = fromString("9706ba78-2df2-4b37-a573-04367dc6f0ea")
            val itemId = fromString("0a674611-c867-44df-b7e0-5e5af31f7b56")

            /* When */
            val playlist = repository.removeFromPlaylist(playlistId, itemId)

            /* Then */
            assertThat(playlist.items).hasSize(0)
        }
    }
}
