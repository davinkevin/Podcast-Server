package com.github.davinkevin.podcastserver.business


import com.github.davinkevin.podcastserver.service.JdomService
import com.nhaarman.mockitokotlin2.*
import io.vavr.API.Set
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.WatchList
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.WatchListRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@ExtendWith(MockitoExtension::class)
class WatchListBusinessTest {

    @Mock lateinit var watchListRepository: WatchListRepository
    @Mock lateinit var itemRepository: ItemRepository
    @Mock lateinit var jdomService: JdomService
    @InjectMocks lateinit var watchListBusiness: WatchListBusiness

    @Test
    fun `should find all`() {
        /* Given */
        whenever(watchListRepository.findAll()).thenReturn(listOf())

        /* When */
        val all = watchListBusiness.findAll()

        /* Then */
        assertThat(all).isEmpty()
        verify(watchListRepository, only()).findAll()
    }

    @Test
    fun `should find all playlist with specified item`() {
        /* Given */
        val uuid = UUID.randomUUID()
        val item = Item().apply { id = uuid }
        val p1 = WatchList().apply {
            id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
            name = "First"
        }
        val p2 = WatchList().apply {
            id = UUID.fromString("86faa982-f462-400a-bc9b-91eb299910b6")
            name = "Second"
        }
        val watchLists = Set(p1, p2)

        whenever(itemRepository.findById(eq(uuid))).thenReturn(Optional.of(item))
        whenever(watchListRepository.findContainsItem(eq(item))).thenReturn(watchLists)

        /* When */
        val watchListOfItem = watchListBusiness.findContainsItem(uuid)

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchLists)
        verify(itemRepository, only()).findById(eq(uuid))
        verify(watchListRepository, only()).findContainsItem(eq(item))
    }

    @Test
    fun `should return empty result if item wasn't found`() {
        /* Given */
        val uuid = UUID.randomUUID()

        whenever(itemRepository.findById(eq(uuid))).thenReturn(Optional.empty())

        /* When */
        val watchListOfItem = watchListBusiness.findContainsItem(uuid)

        /* Then */
        assertThat(watchListOfItem).isEmpty()
        verify(itemRepository, only()).findById(eq(uuid))
    }

    @Test
    fun `should add item to playlist`() {
        /* Given */
        val uuid = UUID.randomUUID()
        val item = Item().apply {
                id = uuid
                watchLists = mutableSetOf()
        }
        val watchList = WatchList().apply {
                id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
                name = "First"
                items = mutableSetOf()
        }

        whenever(itemRepository.findById(eq(uuid))).thenReturn(Optional.of(item))
        whenever(watchListRepository.findById(eq(watchList.id))).thenReturn(Optional.of(watchList))
        whenever(watchListRepository.save(any<WatchList>())).then { it.arguments[0] }

        /* When */
        val watchListOfItem = watchListBusiness.add(watchList.id, uuid)

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchList)
        assertThat(watchListOfItem.items).contains(item)
        verify(itemRepository, only()).findById(eq(uuid))
        verify(watchListRepository, times(1)).findById(eq(watchList.id))
        verify(watchListRepository, times(1)).save(eq(watchList))
    }

    @Test
    fun `should throw exception when trying to add a non existing item to playlist`() {
        /* Given */
        val uuid = UUID.randomUUID()
        val watchList = WatchList().apply {
            id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
            name = "First"
            items = mutableSetOf()
        }

        whenever(itemRepository.findById(eq(uuid))).thenReturn(Optional.empty())
        whenever(watchListRepository.findById(eq(watchList.id))).thenReturn(Optional.of(watchList))

        /* When */
        assertThatThrownBy { watchListBusiness.add(watchList.id, uuid) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Item with ID $uuid not found")

        /* Then */
        verify(watchListRepository, times(1)).findById(eq(watchList.id))
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
        val watchList = WatchList().apply {
            id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
            name = "First"
            items = mutableSetOf()
        }

        whenever(itemRepository.findById(eq(uuid))).thenReturn(Optional.of(item))
        whenever(watchListRepository.findById(eq(watchList.id))).thenReturn(Optional.of(watchList))
        whenever(watchListRepository.save(any<WatchList>())).then { it.arguments[0] }

        /* When */
        val watchListOfItem = watchListBusiness.remove(watchList.id, uuid)

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchList)
        assertThat(watchListOfItem.items).doesNotContain(item)
        verify(itemRepository, only()).findById(ArgumentMatchers.eq(uuid))
        verify(watchListRepository, times(1)).findById(eq(watchList.id))
        verify(watchListRepository, times(1)).save(eq(watchList))
    }

    @Test
    fun `should throw exception when trying to remove a non existing item to playlist`() {
        /* Given */
        val uuid = UUID.randomUUID()
        val watchList = WatchList().apply {
            id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
            name = "First"
            items = mutableSetOf()
        }

        whenever(itemRepository.findById(eq(uuid))).thenReturn(Optional.empty())
        whenever(watchListRepository.findById(eq(watchList.id))).thenReturn(Optional.of(watchList))

        /* When */
        assertThatThrownBy { watchListBusiness.remove(watchList.id, uuid) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Item with ID $uuid not found")

        /* Then */
        verify(watchListRepository, times(1)).findById(eq(watchList.id))
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

        /* When */
        watchListBusiness.save(watchList)

        /* Then */
        verify(watchListRepository, only()).save(eq(watchList))
    }

    @Test
    fun `should find one by id`() {
        /* Given */
        val watchList = WatchList().apply {
            id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")
            name = "First"
            items = mutableSetOf()
        }

        whenever(watchListRepository.findById(eq(watchList.id))).thenReturn(Optional.of(watchList))

        /* When */
        val aWatchList = watchListBusiness.findOne(watchList.id)

        /* Then */
        assertThat(aWatchList).isSameAs(watchList)
        verify(watchListRepository, only()).findById(eq(watchList.id))
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
        whenever(jdomService.watchListToXml(eq(watchList), anyString())).thenReturn("anXml")
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
