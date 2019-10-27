package com.github.davinkevin.podcastserver.entity

import com.fasterxml.jackson.annotation.JsonView
import java.util.*

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
class WatchList {

    var id: UUID? = null
    var name: String? = null

    @JsonView(WatchListDetailsListView::class)
    var items = mutableSetOf<Item>()

    interface WatchListDetailsListView
}
