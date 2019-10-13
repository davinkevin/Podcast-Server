package com.github.davinkevin.podcastserver.playlist

import java.net.URI
import java.util.*

/**
 * Created by kevin on 2019-07-01
 */
data class Playlist(val id: UUID, val name: String)

data class PlaylistWithItems(val id: UUID, val name: String, val items: Collection<Item>) {

    data class Item(
            val id: UUID,
            val title: String,
            val fileName: String?,

            val description: String?,
            val mimeType: String?,

            val podcast: Podcast,
            val cover: Cover) {

        data class Podcast(val id: UUID, val title: String)
        data class Cover (val id: UUID, val width: Int, val height: Int, val url: URI)
    }
}
