package com.github.davinkevin.podcastserver.business.find

import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.selector.FinderSelector
import org.springframework.stereotype.Component

/**
 * Created by kevin on 22/02/15 for Podcast Server
 */
@Component
class FindPodcastBusiness(val finderSelector: FinderSelector) {
    fun fetchPodcastInfoByUrl(url: String): Podcast = finderSelector.of(url).find(url)
}
