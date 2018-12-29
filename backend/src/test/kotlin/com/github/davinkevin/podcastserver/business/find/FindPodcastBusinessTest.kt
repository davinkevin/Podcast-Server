package com.github.davinkevin.podcastserver.business.find

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.selector.FinderSelector
import lan.dk.podcastserver.manager.worker.Finder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Created by kevin on 01/08/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class FindPodcastBusinessTest {

    @Mock lateinit var finderSelector: FinderSelector
    @InjectMocks lateinit var findPodcastBusiness: FindPodcastBusiness

    @Test
    fun `should not find any finder`() {
        /* Given */
        val fakeUrl = "http://any.fake.url/"
        whenever(finderSelector.of(fakeUrl)).thenReturn(FinderSelector.NO_OP_FINDER)
        /* When */
        val podcast = findPodcastBusiness.fetchPodcastInfoByUrl(fakeUrl)
        /* Then */
        assertThat(podcast).isSameAs(Podcast.DEFAULT_PODCAST)
        verify(finderSelector, times(1)).of(fakeUrl)
    }

    @Test
    fun `should find a finder`() {
        /* Given */
        val fakeUrl = "http://any.fake.url/"
        val finder = mock<Finder>()
        whenever(finderSelector.of(fakeUrl)).thenReturn(finder)
        whenever(finder.find(fakeUrl)).thenReturn(Podcast())

        /* When */
        val podcast = findPodcastBusiness.fetchPodcastInfoByUrl(fakeUrl)

        /* Then */
        assertThat(podcast)
                .isNotNull()
                .isEqualTo(Podcast())
        verify(finderSelector, times(1)).of(fakeUrl)
        verify(finder, times(1)).find(fakeUrl)
    }
}
