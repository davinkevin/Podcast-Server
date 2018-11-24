package com.github.davinkevin.podcastserver.entity

import com.fasterxml.jackson.annotation.JsonView
import lan.dk.podcastserver.entity.Item
import java.util.*
import javax.persistence.*

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Entity
class WatchList {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    var id: UUID? = null

    var name: String? = null

    @ManyToMany
    @JsonView(WatchListDetailsListView::class)
    var items = mutableSetOf<Item>()

    fun add(item: Item) {
        item.watchLists.add(this)
        items.add(item)
    }

    fun remove(item: Item) {
        item.watchLists.remove(this)
        items.remove(item)
    }

    interface WatchListDetailsListView
}
