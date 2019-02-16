package com.github.davinkevin.podcastserver.business.stats

import com.github.davinkevin.podcastserver.item.ItemRepositoryV2 as ItemRepository
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.collection.Set
import org.springframework.stereotype.Component
import java.util.*

/**
 * Created by kevin on 28/04/15 for HackerRank problem
 */
@Component
class StatsBusiness(val itemRepository: ItemRepository) {

    fun allStatsByTypeAndDownloadDate(numberOfMonth: Int) =
            itemRepository.allStatsByTypeAndDownloadDate(numberOfMonth).toVΛVΓ()

    fun allStatsByTypeAndCreationDate(numberOfMonth: Int) =
            itemRepository.allStatsByTypeAndCreationDate(numberOfMonth).toVΛVΓ()

    fun allStatsByTypeAndPubDate(numberOfMonth: Int) =
            itemRepository.allStatsByTypeAndPubDate(numberOfMonth).toVΛVΓ()
}
