package com.github.davinkevin.podcastserver.playlist

import com.github.davinkevin.podcastserver.database.Tables.WATCH_LIST
import com.github.davinkevin.podcastserver.extension.repository.fetchAsFlux
import org.jooq.DSLContext

class PlaylistRepositoryV2(
        private val query: DSLContext
) {

    fun findAll() = query
            .select(WATCH_LIST.ID, WATCH_LIST.NAME)
            .from(WATCH_LIST)
            .fetchAsFlux()
            .map { Playlist(
                    id = it[WATCH_LIST.ID],
                    name = it[WATCH_LIST.NAME]
            ) }

}
