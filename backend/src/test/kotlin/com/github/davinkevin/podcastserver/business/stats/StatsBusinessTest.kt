package com.github.davinkevin.podcastserver.business.stats

import com.github.davinkevin.podcastserver.business.PodcastBusiness
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import io.vavr.API
import io.vavr.collection.HashSet
import io.vavr.collection.Set
import lan.dk.podcastserver.repository.ItemRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Created by kevin on 05/08/15 for Podcast Server
 */
@Disabled
@ExtendWith(MockitoExtension::class)
class StatsBusinessTest {

    @Mock lateinit var itemRepository: ItemRepository
    @Mock lateinit var podcastBusiness: PodcastBusiness
    @Mock lateinit var updaterSelector: UpdaterSelector
    @InjectMocks lateinit var statsBusiness: StatsBusiness

    @Nested
    @DisplayName("For All")
    inner class ForAll {

        @BeforeEach
        fun beforeEach() {
            whenever(updaterSelector.types()).thenReturn(API.Set(RSS, BE_IN_SPORT, CANAL_PLUS, YOUTUBE))

            doReturn(generateItems(5)).whenever(itemRepository).findByTypeAndExpression(eq(RSS), any())
            doReturn(HashSet.empty<Item>()).whenever(itemRepository).findByTypeAndExpression(eq(BE_IN_SPORT), any())
            doReturn(generateItems(10)).whenever(itemRepository).findByTypeAndExpression(eq(CANAL_PLUS), any())
            doReturn(generateItems(50)).whenever(itemRepository).findByTypeAndExpression(eq(YOUTUBE), any())
        }

        @Test
        fun `should stats all by download date`() {
            /* Given */

            /* When */
            val statsPodcastTypes = statsBusiness.allStatsByTypeAndDownloadDate(1)

            /* Then */
            assertThat(statsPodcastTypes).hasSize(3)
            assertThat(statsPodcastTypes[0].values).hasSize(10)
            assertThat(statsPodcastTypes[1].values).hasSize(5)
            assertThat(statsPodcastTypes[2].values).hasSize(50)
        }

        @Test
        fun `should stats all by creation date`() {
            /* Given */

            /* When */
            val statsPodcastTypes = statsBusiness.allStatsByTypeAndCreationDate(1)

            /* Then */
            assertThat(statsPodcastTypes).hasSize(3)
            assertThat(statsPodcastTypes[0].values).hasSize(10)
            assertThat(statsPodcastTypes[1].values).hasSize(5)
            assertThat(statsPodcastTypes[2].values).hasSize(50)
        }

        @Test
        fun `should stats all by publication date`() {
            /* Given */

            /* When */
            val statsPodcastTypes = statsBusiness.allStatsByTypeAndPubDate(1)

            /* Then */
            assertThat(statsPodcastTypes).hasSize(3)
            assertThat(statsPodcastTypes[0].values).hasSize(10)
            assertThat(statsPodcastTypes[1].values).hasSize(5)
            assertThat(statsPodcastTypes[2].values).hasSize(50)
        }
    }

    private fun generateItems(numberOfItem: Int): Set<Item> =
            (1..numberOfItem)
            .map { it.toLong()}
            .map {
                Item().apply {
                    id = UUID.randomUUID()
                    pubDate = ZonedDateTime.now().minusDays(it)
                    downloadDate = ZonedDateTime.now().minusDays(it)
                    creationDate = ZonedDateTime.now().minusDays(it)
                }
            }
            .toSet()
            .toVΛVΓ()

    companion object {
        private val YOUTUBE = Type("Youtube", "Youtube")
        private val CANAL_PLUS = Type("CanalPlus", "CanalPlus")
        private val BE_IN_SPORT = Type("BeInSport", "BeInSport")
        private val RSS = Type("RSS", "RSS")
    }
}
