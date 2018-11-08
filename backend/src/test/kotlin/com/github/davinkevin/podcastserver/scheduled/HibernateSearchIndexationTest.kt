package com.github.davinkevin.podcastserver.scheduled

import com.github.davinkevin.podcastserver.business.ItemBusiness
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
class HibernateSearchIndexationTest {

    @Mock lateinit var itemBusiness: ItemBusiness
    @InjectMocks lateinit var hibernateSearchIndexation: HibernateSearchIndexation

    @Test
    fun should_refresh_index() {
        /* When */
        hibernateSearchIndexation.refreshIndex()
        /* Then */
        verify(itemBusiness, times(1)).reindex()
    }
}
