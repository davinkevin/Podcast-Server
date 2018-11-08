package com.github.davinkevin.podcastserver.scheduled

import com.github.davinkevin.podcastserver.business.ItemBusiness
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Created by kevin on 22/08/2014.
 */
@Component
class HibernateSearchIndexation(private val itemBusiness: ItemBusiness) {

    @Scheduled(fixedDelay = 86400000)
    fun refreshIndex() = itemBusiness.reindex()

}
