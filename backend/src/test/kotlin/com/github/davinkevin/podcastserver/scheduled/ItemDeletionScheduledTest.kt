package com.github.davinkevin.podcastserver.scheduled

import com.github.davinkevin.podcastserver.business.update.UpdatePodcastBusiness
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class ItemDeletionScheduledTest {

    @Mock lateinit var updatePodcastBusiness: UpdatePodcastBusiness
    @InjectMocks lateinit var itemDeletionScheduled: ItemDeletionScheduled

    @Test
    fun `should delete old item`() {
        /* When */ itemDeletionScheduled.deleteOldItem()
        /* Then */ verify<UpdatePodcastBusiness>(updatePodcastBusiness, times(1)).deleteOldEpisode()
    }

    @Test
    fun `should delete old cover`() {
        /* When */ itemDeletionScheduled.deleteOldCover()
        /* Then */ verify<UpdatePodcastBusiness>(updatePodcastBusiness, times(1)).deleteOldCover()
    }

}
