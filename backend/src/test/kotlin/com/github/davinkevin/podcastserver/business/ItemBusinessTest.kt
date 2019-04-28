package com.github.davinkevin.podcastserver.business

import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.entity.Tag
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.querydsl.core.types.Predicate
import io.vavr.API.Set
import io.vavr.collection.List
import lan.dk.podcastserver.repository.ItemRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.util.FileSystemUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Created by kevin on 02/08/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class ItemBusinessTest {

    @Mock lateinit var itemDownloadManager: ItemDownloadManager
    @Mock lateinit var podcastServerParameters: PodcastServerParameters
    @Mock lateinit var itemRepository: ItemRepository
    @Mock lateinit var podcastBusiness: PodcastBusiness
    @Mock lateinit var mimeTypeService: MimeTypeService
    @InjectMocks lateinit var itemBusiness: ItemBusiness

    @BeforeEach
    fun beforeEach() {
        FileSystemUtils.deleteRecursively(Paths.get(ROOT_FOLDER).toFile())
    }

    @Test
    fun `should find all by page`() {
        /* Given */
        val pageRequest = PageRequest.of(1, 3)
        val page = PageImpl(listOf<Item>())
        whenever(itemRepository.findAll(pageRequest)).thenReturn(page)

        /* When */
        val pageResponse = itemBusiness.findAll(pageRequest)

        /* Then */
        assertThat(pageResponse).isSameAs(page)
        verify(itemRepository, times(1)).findAll(eq(pageRequest))
    }

    @Test
    fun `should find by id`() {
        /* Given */
        val idOfItem = UUID.randomUUID()
        val item = Item()
        whenever(itemRepository.findById(idOfItem)).thenReturn(Optional.of(item))

        /* When */
        val savedItem = itemBusiness.findOne(idOfItem)

        /* Then */
        assertThat(savedItem).isSameAs(item)
        verify(itemRepository, times(1)).findById(idOfItem)
    }

    @Test
    fun `should throw exception if dont find item by id`() {
        /* Given */
        val idOfItem = UUID.randomUUID()
        whenever(itemRepository.findById(idOfItem)).thenReturn(Optional.empty())

        /* When */
        assertThatThrownBy { itemBusiness.findOne(idOfItem) }

        /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Item with ID $idOfItem not found")
        verify(itemRepository, times(1)).findById(idOfItem)
    }

    @Test
    fun `should delete`() {
        /* Given */
        val idOfItem = UUID.randomUUID()
        val p = Podcast().apply { items = mutableSetOf() }
        val item = Item().apply { podcast = p }
        p.items!!.add(item)

        whenever(itemRepository.findById(idOfItem)).thenReturn(Optional.of(item))

        /* When */
        itemBusiness.delete(idOfItem)

        /* Then */
        verify(itemRepository, times(1)).findById(idOfItem)
        verify(itemDownloadManager, times(1)).removeItemFromQueueAndDownload(item)
        verify(itemRepository, times(1)).delete(item)
        assertThat(p.items).isEmpty()
    }

    @Test
    fun `should find page in podcast`() {
        /* Given */
        val idPodcast = UUID.randomUUID()
        val pageRequest = PageRequest.of(0, 20)
        val pageOfItem = PageImpl(listOf<Item>())
        whenever(itemRepository.findByPodcast(idPodcast, pageRequest)).thenReturn(pageOfItem)

        /* When */
        val pageOfPodcast = itemBusiness.findByPodcast(idPodcast, pageRequest)

        /* Then */
        assertThat(pageOfPodcast.content).isEqualTo(listOf<Item>())
        verify(itemRepository, times(1)).findByPodcast(idPodcast, pageRequest)
    }

    @Test
    @Throws(IOException::class, URISyntaxException::class)
    fun `should add item by upload`() {
        /* Given */
        val idPodcast = UUID.randomUUID()
        val uploadedFile = mock<MultipartFile>()
        val podcast = Podcast().apply { description = "aDescription"; title = "aPodcast" }
        val length = 123456789L
        val aMimeType = "audio/type"
        val title = "aPodcast - 2015-09-10 - aTitle.mp3"
        val itemFilePath = Paths.get(ROOT_FOLDER, podcast.title, title)!!
        Files.createDirectories(itemFilePath.parent)
        Files.createFile(itemFilePath)

        whenever(podcastBusiness.findOne(idPodcast)).thenReturn(podcast)
        whenever(uploadedFile.originalFilename).thenReturn(title)
        whenever(podcastServerParameters.rootfolder).thenReturn(Paths.get(ROOT_FOLDER))
        whenever(itemRepository.save(any())).then { it.arguments[0] }
        whenever(podcastBusiness.save(any())).then { it.arguments[0] }
        whenever(uploadedFile.size).thenReturn(length)
        whenever(mimeTypeService.getMimeType(anyString())).thenReturn(aMimeType)

        /* When */
        val item = itemBusiness.addItemByUpload(idPodcast, uploadedFile)

        /* Then */
        assertThat(item.title).isEqualTo("aTitle")
        assertThat(item.pubDate).isEqualTo(ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(title.split(" - ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1], DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.of(0, 0)), ZoneId.systemDefault()))
        assertThat(item.url).isEqualTo(null)
        assertThat(item.length).isEqualTo(length)
        assertThat(item.mimeType).isEqualTo(aMimeType)
        assertThat(item.description).isEqualTo("aDescription")
        assertThat(item.fileName).isEqualTo(title)
        assertThat(item.podcast).isEqualTo(podcast)
        assertThat(item.status).isEqualTo(Status.FINISH)

        assertThat(podcast.items).contains(item)
        verify(podcastServerParameters, times(1)).rootfolder
        verify(mimeTypeService, times(1)).getMimeType("mp3")
        verify(podcastBusiness, times(1)).findOne(idPodcast)
        verify(podcastBusiness, times(1)).save(podcast)
        verify(itemRepository, times(1)).save(item)
    }

    companion object {
        private const val ROOT_FOLDER = "/tmp/podcast"
    }
}
