package com.github.davinkevin.podcastserver.scheduled

import com.github.davinkevin.podcastserver.business.update.UpdatePodcastBusiness
import lan.dk.podcastserver.manager.ItemDownloadManager
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
class UpdateScheduledTest {

    @Mock lateinit var updatePodcastBusiness: UpdatePodcastBusiness
    @Mock lateinit var IDM: ItemDownloadManager
    @InjectMocks lateinit var updateScheduled: UpdateScheduled

    @Test
    fun should_update_and_download() {
        /* When */
        updateScheduled.updateAndDownloadPodcast()
        /* Then */
        verify(updatePodcastBusiness, times(1)).updatePodcast()
        verify(IDM, times(1)).launchDownload()
    }
}
