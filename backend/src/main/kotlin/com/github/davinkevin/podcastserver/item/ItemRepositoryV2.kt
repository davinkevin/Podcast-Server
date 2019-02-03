package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.business.stats.NumberOfItemByDateWrapper
import com.github.davinkevin.podcastserver.business.stats.StatsPodcastType
import com.github.davinkevin.podcastserver.database.Tables.ITEM
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.database.tables.records.ItemRecord
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL.*
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Duration
import java.time.ZonedDateTime.now
import java.util.*

/**
 * Created by kevin on 2019-02-03
 */
@Repository
class ItemRepositoryV2(val query: DSLContext) {

    fun findStatOfCreationDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.CREATION_DATE)
    fun findStatOfPubDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.PUB_DATE)
    fun findStatOfDownloadDate(pid: UUID, month: Int) = findStatOfOnField(pid, month, ITEM.DOWNLOAD_DATE)

    private fun findStatOfOnField(pid: UUID, month: Int, field: TableField<ItemRecord, Timestamp>): Set<NumberOfItemByDateWrapper> {
        val date = trunc(field)
        val startDate = now().minusMonths(month.toLong())
        val numberOfDays = Duration.between(startDate, now()).toDays()

        return query
                .select(count(), date)
                .from(ITEM)
                .where(ITEM.PODCAST_ID.eq(pid))
                .and(field.isNotNull)
                .and(dateDiff(currentDate(), date(field)).lessThan(numberOfDays.toInt()))
                .groupBy(date)
                .orderBy(date.desc())
                .fetch { NumberOfItemByDateWrapper(it.component2().toLocalDateTime().toLocalDate(), it.component1()) }
                .toSet()
    }

    /*
    select P.TYPE, count(*) as numberOfItem, parsedatetime(formatdatetime("PUBLIC"."ITEM"."PUB_DATE", 'yyyy-MM-dd'), 'yyyy-MM-dd')
from "PUBLIC"."ITEM"
  INNER JOIN PODCAST P on ITEM.PODCAST_ID = P.ID
where (
  --"PUBLIC"."ITEM"."PODCAST_ID" = '8e99045e-c685-4757-9f93-d67d6d125332'and
  "PUBLIC"."ITEM"."PUB_DATE" is not null and
  datediff('day', cast("PUBLIC"."ITEM"."PUB_DATE" as date), current_date()) < 184)
group by P.TYPE, parsedatetime(formatdatetime("PUBLIC"."ITEM"."PUB_DATE", 'yyyy-MM-dd'), 'yyyy-MM-dd')
order by parsedatetime(formatdatetime("PUBLIC"."ITEM"."PUB_DATE", 'yyyy-MM-dd'), 'yyyy-MM-dd') desc
     */

    fun allStatsByTypeAndCreationDate(numberOfMonth: Int) = allStatsByTypeAndField(numberOfMonth, ITEM.CREATION_DATE)
    fun allStatsByTypeAndPubDate(numberOfMonth: Int) = allStatsByTypeAndField(numberOfMonth, ITEM.PUB_DATE)
    fun allStatsByTypeAndDownloadDate(numberOfMonth: Int) = allStatsByTypeAndField(numberOfMonth, ITEM.DOWNLOAD_DATE)

    private fun allStatsByTypeAndField(month: Int, field: TableField<ItemRecord, Timestamp>): List<StatsPodcastType> {
        val date = trunc(field)
        val startDate = now().minusMonths(month.toLong())
        val numberOfDays = Duration.between(startDate, now()).toDays()

        return query
                .select(PODCAST.TYPE, count(), date)
                .from(ITEM.innerJoin(PODCAST).on(ITEM.PODCAST_ID.eq(PODCAST.ID)))
                .where(field.isNotNull)
                .and(dateDiff(currentDate(), date(field)).lessThan(numberOfDays.toInt()))
                .groupBy(PODCAST.TYPE, date)
                .orderBy(date.desc())
                .groupBy { it[PODCAST.TYPE] }
                .map { StatsPodcastType(
                            type = it.key,
                            values = it.value
                                    .map { (_, number, date) -> NumberOfItemByDateWrapper(date.toLocalDateTime().toLocalDate(), number) }
                                    .toSet()
                                    .toVΛVΓ()
                    )
                }

    }
}