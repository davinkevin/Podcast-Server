package com.github.davinkevin.podcastserver.podcast

import com.github.davinkevin.podcastserver.database.Tables.COVER
import com.github.davinkevin.podcastserver.database.Tables.PODCAST
import com.github.davinkevin.podcastserver.extension.repository.fetchOneAsMono
import org.jooq.DSLContext
import reactor.core.publisher.Mono
import java.util.*

class PodcastRepositoryV2(private val query: DSLContext) {

    fun findById(id: UUID) = Mono.defer {
        query
                .select(
                        PODCAST.ID, PODCAST.TITLE,
                        COVER.ID, COVER.URL, COVER.HEIGHT, COVER.WIDTH
                )
                .from(PODCAST.innerJoin(COVER).on(PODCAST.COVER_ID.eq(COVER.ID)))
                .where(PODCAST.ID.eq(id))
                .fetchOneAsMono()
                .map {
                    val c = CoverForPodcast(it[COVER.ID], it[COVER.URL], it[COVER.WIDTH], it[COVER.HEIGHT])

                    Podcast(it[PODCAST.ID], it[PODCAST.TITLE], c)
                }
    }

}
