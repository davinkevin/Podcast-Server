package com.github.davinkevin.podcastserver.business


import com.github.davinkevin.podcastserver.entity.WatchList
import com.github.davinkevin.podcastserver.service.JdomService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.only
import com.nhaarman.mockitokotlin2.same
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.repository.WatchListRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@ExtendWith(MockitoExtension::class)
class PlaylistBusinessTest {

    @Mock lateinit var watchListRepository: WatchListRepository
    @Mock lateinit var jdomService: JdomService
    @InjectMocks lateinit var watchListBusiness: WatchListBusiness

    @Test
    fun `should delete`() {
        /* Given */
        val id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")

        /* When */
        watchListBusiness.delete(id)

        /* Then */
        verify(watchListRepository, only()).deleteById(eq(id))
    }

    @Test
    fun `should generate watchlist as xml`() {
        /* Given */
        val uuid = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
        val domain = "http://localhost"
        val watchList = WatchList().apply {
            id = uuid
            name = "First"
            items = mutableSetOf()
        }
        whenever(jdomService.watchListToXml(eq(watchList), any())).thenReturn("anXml")
        whenever(watchListRepository.findById(eq(uuid))).thenReturn(Optional.of(watchList))

        /* When */
        val s = watchListBusiness.asRss(uuid, domain)

        /* Then */
        assertThat(s).isEqualTo("anXml")
        verify(watchListRepository).findById(eq(uuid))
        verify(jdomService).watchListToXml(same(watchList), eq(domain))
    }

    @Test
    fun `should throw excpetion when trying to generate xml from a non existing watchlist`() {
        /* Given */
        val uuid = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
        val domain = "http://localhost"
        whenever(watchListRepository.findById(eq(uuid))).thenReturn(Optional.empty())

        /* When */
        assertThatThrownBy { watchListBusiness.asRss(uuid, domain) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Rss generation of watchlist $uuid caused Error")

        /* Then */
        verify(watchListRepository).findById(eq(uuid))
    }

    @AfterEach
    fun afterEach() {
        verifyNoMoreInteractions(watchListRepository)
    }

}
