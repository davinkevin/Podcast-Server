package com.github.davinkevin.podcastserver.digest

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.database.Tables.COVER
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.entity.fromDb
import com.github.davinkevin.podcastserver.item.Item
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.partitionBy
import org.jooq.impl.DSL.rowNumber
import java.net.URI
import java.time.OffsetDateTime

class DigestRepository(private val query: DSLContext) {

    /**
     * Podcasts that published an item since [since], freshness-ordered (the
     * podcast with the most-recent item first). Each podcast carries the
     * real total of items in the window ([DigestPodcast.itemCount]) but only
     * its [maxItemsPerPodcast] most-recent items, so a prolific publisher
     * doesn't blow up the payload.
     *
     * Single query: a CTE tags every in-window item with its rank within its
     * podcast (newest first), the podcast's item count, and the podcast's most
     * recent pubDate (all via window functions); the outer query keeps the
     * top-N per podcast and joins the item cover + the podcast (and its cover).
     */
    fun findActivePodcastsSince(since: OffsetDateTime, maxItemsPerPodcast: Int): List<DigestPodcast> {
        val rank = rowNumber()
            .over(partitionBy(ITEM.PODCAST_ID).orderBy(ITEM.PUB_DATE.desc(), ITEM.ID.asc()))
            .`as`("rank")
        val itemCount = count()
            .over(partitionBy(ITEM.PODCAST_ID))
            .`as`("item_count")
        val lastPubDate = max(ITEM.PUB_DATE)
            .over(partitionBy(ITEM.PODCAST_ID))
            .`as`("last_pub_date")

        val ranked = name("RANKED_ITEMS").`as`(
            query
                .select(
                    ITEM.ID, ITEM.TITLE, ITEM.URL,
                    ITEM.PUB_DATE, ITEM.DOWNLOAD_DATE, ITEM.CREATION_DATE,
                    ITEM.DESCRIPTION, ITEM.MIME_TYPE, ITEM.LENGTH, ITEM.FILE_NAME, ITEM.STATUS,
                    ITEM.PODCAST_ID, ITEM.COVER_ID,
                    rank, itemCount, lastPubDate,
                )
                .from(ITEM)
                .where(ITEM.PUB_DATE.greaterOrEqual(since))
        )

        return query
            .with(ranked)
            .select(
                ranked.field(ITEM.ID), ranked.field(ITEM.TITLE), ranked.field(ITEM.URL),
                ranked.field(ITEM.PUB_DATE), ranked.field(ITEM.DOWNLOAD_DATE), ranked.field(ITEM.CREATION_DATE),
                ranked.field(ITEM.DESCRIPTION), ranked.field(ITEM.MIME_TYPE), ranked.field(ITEM.LENGTH),
                ranked.field(ITEM.FILE_NAME), ranked.field(ITEM.STATUS),
                ranked.field(itemCount),

                COVER.ID, COVER.URL, COVER.WIDTH, COVER.HEIGHT,

                PODCAST.ID, PODCAST.TITLE,
                PODCAST.cover().ID, PODCAST.cover().URL, PODCAST.cover().WIDTH, PODCAST.cover().HEIGHT,
            )
            .from(
                ranked
                    .innerJoin(COVER).on(ranked.field(ITEM.COVER_ID)?.eq(COVER.ID))
                    .innerJoin(PODCAST).on(ranked.field(ITEM.PODCAST_ID)?.eq(PODCAST.ID))
            )
            .where(ranked.field(rank)!!.le(maxItemsPerPodcast))
            .orderBy(ranked.field(lastPubDate)!!.desc(), PODCAST.ID, ranked.field(rank))
            .fetch()
            .map { r ->
                val item = Item(
                    id = r[ranked.field(ITEM.ID)]!!,
                    title = r[ranked.field(ITEM.TITLE)]!!,
                    url = r[ranked.field(ITEM.URL)],
                    pubDate = r[ranked.field(ITEM.PUB_DATE)],
                    downloadDate = r[ranked.field(ITEM.DOWNLOAD_DATE)],
                    creationDate = r[ranked.field(ITEM.CREATION_DATE)],
                    description = r[ranked.field(ITEM.DESCRIPTION)],
                    mimeType = r[ranked.field(ITEM.MIME_TYPE)]!!,
                    length = r[ranked.field(ITEM.LENGTH)],
                    fileName = r[ranked.field(ITEM.FILE_NAME)],
                    status = r[ranked.field(ITEM.STATUS)]!!.fromDb(),
                    podcast = Item.Podcast(r[PODCAST.ID]!!, r[PODCAST.TITLE]!!, null),
                    cover = Item.Cover(r[COVER.ID]!!, URI(r[COVER.URL]), r[COVER.WIDTH]!!, r[COVER.HEIGHT]!!),
                )
                DigestRow(
                    item = item,
                    podcastId = r[PODCAST.ID]!!,
                    podcastTitle = r[PODCAST.TITLE]!!,
                    podcastCover = Cover(
                        id = r[PODCAST.cover().ID]!!,
                        url = URI(r[PODCAST.cover().URL]),
                        height = r[PODCAST.cover().HEIGHT]!!,
                        width = r[PODCAST.cover().WIDTH]!!,
                    ),
                    itemCount = r[ranked.field(itemCount)]!!,
                )
            }
            // groupBy preserves first-encounter order, which the ORDER BY made
            // freshness order — so the resulting list stays freshest-first.
            .groupBy { it.podcastId }
            .map { (_, rows) ->
                val first = rows.first()
                DigestPodcast(
                    id = first.podcastId,
                    title = first.podcastTitle,
                    cover = first.podcastCover,
                    itemCount = first.itemCount,
                    items = rows.map { it.item },
                )
            }
    }

    private data class DigestRow(
        val item: Item,
        val podcastId: java.util.UUID,
        val podcastTitle: String,
        val podcastCover: Cover,
        val itemCount: Int,
    )
}
