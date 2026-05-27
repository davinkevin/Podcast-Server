package com.github.davinkevin.podcastserver.digest

import com.github.davinkevin.podcastserver.database.Tables.COVER
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.database.enums.ItemStatus
import com.github.davinkevin.podcastserver.extension.spring.NestedSpringTest
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.impl.DSL.insertInto
import org.jooq.impl.DSL.truncate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jooq.test.autoconfigure.JooqTest
import org.springframework.context.annotation.Import
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.util.UUID.fromString

@JooqTest
@NestedSpringTest
@Import(DigestRepository::class)
class DigestRepositoryTest(
    @Autowired val repository: DigestRepository,
    @Autowired val query: DSLContext,
) {

    // Podcasts
    private val appLoad = fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82")
    private val comics = fromString("ef85dcd3-758c-473f-a8fc-b82104762d9d")
    private val geek = fromString("a3e2b9d1-0c4f-4f6a-9b1e-2d3c4e5f6a7b")

    // Covers
    private val appLoadCover = fromString("9f050dc4-6a2e-46c3-8276-43098c011e68")
    private val comicsCover = fromString("8ea0373e-7af6-4e15-b0fd-9ec4b10822ec")
    private val geekCover = fromString("1c8c9f3a-2b4d-4e6f-8a0b-1c2d3e4f5a6b")
    private val itemCover = fromString("b0a1c2d3-e4f5-4a6b-8c7d-9e0f1a2b3c4d")

    @BeforeEach
    fun beforeEach() {
        query.batch(
            truncate(ITEM).cascade(),
            truncate(PODCAST).cascade(),
            truncate(COVER).cascade(),
        ).execute()

        query.batch(
            insertInto(COVER, COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT)
                .values(appLoadCover, "https://fake.url.com/appload/cover.png", 100, 100)
                .values(comicsCover, "https://fake.url.com/comics/cover.png", 100, 100)
                .values(geekCover, "https://fake.url.com/geek/cover.png", 100, 100)
                .values(itemCover, "https://fake.url.com/item/cover.png", 200, 200),
            insertInto(PODCAST, PODCAST.ID, PODCAST.TITLE, PODCAST.URL, PODCAST.COVER_ID, PODCAST.HAS_TO_BE_DELETED, PODCAST.TYPE)
                .values(appLoad, "AppLoad", "https://fake.url.com/appload.rss", appLoadCover, false, "RSS")
                .values(comics, "Comics", "https://fake.url.com/comics.rss", comicsCover, false, "RSS")
                .values(geek, "Geek Inc", "https://fake.url.com/geek.rss", geekCover, false, "RSS"),
        ).execute()

        // AppLoad: 3 items within the last 7 days.
        insertItem("00000000-0000-0000-0000-0000000000a1", "A-a", appLoad, now().minusHours(1))
        insertItem("00000000-0000-0000-0000-0000000000a2", "A-b", appLoad, now().minusDays(2))
        insertItem("00000000-0000-0000-0000-0000000000a3", "A-c", appLoad, now().minusDays(3))

        // Comics: 5 items within the last 7 days; the most recent overall.
        insertItem("00000000-0000-0000-0000-0000000000c1", "C-a", comics, now().minusMinutes(30))
        insertItem("00000000-0000-0000-0000-0000000000c2", "C-b", comics, now().minusDays(1))
        insertItem("00000000-0000-0000-0000-0000000000c3", "C-c", comics, now().minusDays(2))
        insertItem("00000000-0000-0000-0000-0000000000c4", "C-d", comics, now().minusDays(4))
        insertItem("00000000-0000-0000-0000-0000000000c5", "C-e", comics, now().minusDays(5))

        // Geek Inc: only an old item, outside the window.
        insertItem("00000000-0000-0000-0000-0000000000e1", "G-a", geek, now().minusDays(10))
    }

    private fun insertItem(id: String, title: String, podcastId: java.util.UUID, pubDate: OffsetDateTime) {
        query.insertInto(
            ITEM, ITEM.ID, ITEM.TITLE, ITEM.URL, ITEM.GUID, ITEM.PODCAST_ID, ITEM.STATUS,
            ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE, ITEM.NUMBER_OF_FAIL, ITEM.COVER_ID, ITEM.MIME_TYPE,
        )
            .values(
                fromString(id), title, "https://fake.url.com/$title", "guid-$title", podcastId, ItemStatus.NOT_DOWNLOADED,
                pubDate, null, pubDate, 0, itemCover, "audio/mp3",
            )
            .execute()
    }

    @Test
    @DisplayName("groups active podcasts freshest-first, excludes out-of-window, exposes real itemCount")
    fun `groups active podcasts`() {
        val result = repository.findActivePodcastsSince(now().minusDays(7), 12)

        // Geek is excluded (only a 10-day-old item); Comics leads (most recent item).
        assertThat(result.map { it.id }).containsExactly(comics, appLoad)

        val comicsDigest = result[0]
        assertThat(comicsDigest.title).isEqualTo("Comics")
        assertThat(comicsDigest.itemCount).isEqualTo(5)
        assertThat(comicsDigest.items).hasSize(5)
        assertThat(comicsDigest.items.map { it.title }).containsExactly("C-a", "C-b", "C-c", "C-d", "C-e")
        // The podcast cover is returned, not the item cover.
        assertThat(comicsDigest.cover.id).isEqualTo(comicsCover)
        assertThat(comicsDigest.items.first().cover.id).isEqualTo(itemCover)

        val appLoadDigest = result[1]
        assertThat(appLoadDigest.itemCount).isEqualTo(3)
        assertThat(appLoadDigest.items.map { it.title }).containsExactly("A-a", "A-b", "A-c")
        assertThat(appLoadDigest.cover.id).isEqualTo(appLoadCover)
    }

    @Test
    @DisplayName("caps items per podcast but keeps the real total in itemCount")
    fun `caps items per podcast`() {
        val result = repository.findActivePodcastsSince(now().minusDays(7), 2)

        assertThat(result.map { it.id }).containsExactly(comics, appLoad)

        val comicsDigest = result[0]
        assertThat(comicsDigest.itemCount).isEqualTo(5)
        assertThat(comicsDigest.items.map { it.title }).containsExactly("C-a", "C-b")

        val appLoadDigest = result[1]
        assertThat(appLoadDigest.itemCount).isEqualTo(3)
        assertThat(appLoadDigest.items.map { it.title }).containsExactly("A-a", "A-b")
    }

    @Test
    @DisplayName("returns nothing when no item falls in the window")
    fun `returns empty when window is too narrow`() {
        val result = repository.findActivePodcastsSince(now().minusMinutes(10), 12)

        assertThat(result).isEmpty()
    }
}
