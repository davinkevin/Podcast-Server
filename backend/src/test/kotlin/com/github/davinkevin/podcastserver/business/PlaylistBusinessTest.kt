package com.github.davinkevin.podcastserver.business


import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.WatchList
import com.github.davinkevin.podcastserver.service.JdomService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.only
import com.nhaarman.mockitokotlin2.same
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.vavr.API.Set
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.WatchListRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
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
    @Mock lateinit var itemRepository: ItemRepository
    @Mock lateinit var jdomService: JdomService
    @InjectMocks lateinit var watchListBusiness: WatchListBusiness

    @Test
    fun `should add item to playlist`() {
        /* Given */
        val uuid = UUID.randomUUID()
        val item = Item().apply {
                id = uuid
                watchLists = mutableSetOf()
        }
        val wId = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
        val watchList = WatchList().apply {
                id = wId
                name = "First"
                items = mutableSetOf()
        }

        whenever(itemRepository.findById(eq(uuid))).thenReturn(Optional.of(item))
        whenever(watchListRepository.findById(eq(wId))).thenReturn(Optional.of(watchList))
        whenever(watchListRepository.save(any<WatchList>())).then { it.arguments[0] }

        /* When */
        val watchListOfItem = watchListBusiness.add(wId, uuid)

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchList)
        assertThat(watchListOfItem.items).contains(item)
        verify(itemRepository, only()).findById(eq(uuid))
        verify(watchListRepository, times(1)).findById(eq(wId))
        verify(watchListRepository, times(1)).save(eq(watchList))
    }

    @Test
    fun `should throw exception when trying to add a non existing item to playlist`() {
        /* Given */
        val uuid = UUID.randomUUID()
        val wId = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
        val watchList = WatchList().apply {
            id = wId
            name = "First"
            items = mutableSetOf()
        }

        whenever(itemRepository.findById(eq(uuid))).thenReturn(Optional.empty())
        whenever(watchListRepository.findById(eq(wId))).thenReturn(Optional.of(watchList))

        /* When */
        assertThatThrownBy { watchListBusiness.add(wId, uuid) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Item with ID $uuid not found")

        /* Then */
        verify(watchListRepository, times(1)).findById(eq(wId))
        verify(itemRepository, only()).findById(eq(uuid))
    }

    @Test
    fun `should remove item to playlist`() {
        /* Given */
        val uuid = UUID.randomUUID()
        val item = Item().apply {
            id = uuid
            watchLists = mutableSetOf()
        }
        val wId = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
        val watchList = WatchList().apply {
            id = wId
            name = "First"
            items = mutableSetOf()
        }

        whenever(itemRepository.findById(eq(uuid))).thenReturn(Optional.of(item))
        whenever(watchListRepository.findById(eq(wId))).thenReturn(Optional.of(watchList))
        whenever(watchListRepository.save(any<WatchList>())).then { it.arguments[0] }

        /* When */
        val watchListOfItem = watchListBusiness.remove(wId, uuid)

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchList)
        assertThat(watchListOfItem.items).doesNotContain(item)
        verify(itemRepository, only()).findById(ArgumentMatchers.eq(uuid))
        verify(watchListRepository, times(1)).findById(eq(wId))
        verify(watchListRepository, times(1)).save(eq(watchList))
    }

    @Test
    fun `should throw exception when trying to remove a non existing item to playlist`() {
        /* Given */
        val uuid = UUID.randomUUID()
        val wId = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
        val watchList = WatchList().apply {
            id = wId
            name = "First"
            items = mutableSetOf()
        }

        whenever(itemRepository.findById(eq(uuid))).thenReturn(Optional.empty())
        whenever(watchListRepository.findById(eq(wId))).thenReturn(Optional.of(watchList))

        /* When */
        assertThatThrownBy { watchListBusiness.remove(wId, uuid) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Item with ID $uuid not found")

        /* Then */
        verify(watchListRepository, times(1)).findById(eq(wId))
        verify(itemRepository, only()).findById(eq(uuid))
    }

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
    fun `should save`() {
        /* Given */
        val watchList = WatchList().apply {
            id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
            name = "First"
            items = mutableSetOf()
        }
        whenever(watchListRepository.save(watchList)).thenReturn(watchList)

        /* When */
        watchListBusiness.save(watchList)

        /* Then */
        verify(watchListRepository, only()).save(watchList)
    }

    @Test
    fun `should find one by id`() {
        /* Given */
        val wId = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
        val watchList = WatchList().apply {
            id = wId
            name = "First"
            items = mutableSetOf()
        }

        whenever(watchListRepository.findById(eq(wId))).thenReturn(Optional.of(watchList))

        /* When */
        val aWatchList = watchListBusiness.findOne(wId)

        /* Then */
        assertThat(aWatchList).isSameAs(watchList)
        verify(watchListRepository, only()).findById(eq(wId))
    }

    @Test
    fun `should throw exception if not found`() {
        /* Given */
        val otherUUID = UUID.fromString("16f7a430-8d4c-45d4-b4ec-67c807b82634")

        whenever(watchListRepository.findById(eq(otherUUID))).thenReturn(Optional.empty())

        /* When */
        assertThatThrownBy { watchListBusiness.findOne(otherUUID) }

        /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Watchlist not found")
        verify(watchListRepository, only()).findById(eq(otherUUID))
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
        verifyNoMoreInteractions(watchListRepository, itemRepository)
    }

}