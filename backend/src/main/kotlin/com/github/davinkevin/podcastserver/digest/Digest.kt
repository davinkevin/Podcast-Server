package com.github.davinkevin.podcastserver.digest

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.item.Item
import java.util.UUID

/**
 * A podcast that published at least one item within the requested window,
 * together with its most-recent items (capped) and the real total of items
 * published in that window.
 *
 * Created for the landing/digest view — see issue #257.
 */
data class DigestPodcast(
    val id: UUID,
    val title: String,
    val cover: Cover,
    val itemCount: Int,
    val items: List<Item>,
)
