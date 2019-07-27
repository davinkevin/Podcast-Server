package com.github.davinkevin.podcastserver.business

import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.repository.ItemRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.util.FileSystemUtils
import java.nio.file.Paths
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

//    @Test
//    @Throws(IOException::class, URISyntaxException::class)
//    fun `should add item by upload`() {
//        /* Given */
//        val idPodcast = UUID.randomUUID()
//        val uploadedFile = mock<MultipartFile>()
//        val podcast = Podcast().apply { description = "aDescription"; title = "aPodcast" }
//        val length = 123456789L
//        val aMimeType = "audio/type"
//        val title = "aPodcast - 2015-09-10 - aTitle.mp3"
//        val itemFilePath = Paths.get(ROOT_FOLDER, podcast.title, title)!!
//        Files.createDirectories(itemFilePath.parent)
//        Files.createFile(itemFilePath)
//
//        whenever(podcastBusiness.findOne(idPodcast)).thenReturn(podcast)
//        whenever(uploadedFile.originalFilename).thenReturn(title)
//        whenever(podcastServerParameters.rootfolder).thenReturn(Paths.get(ROOT_FOLDER))
//        whenever(itemRepository.save(any())).then { it.arguments[0] }
//        whenever(podcastBusiness.save(any())).then { it.arguments[0] }
//        whenever(uploadedFile.size).thenReturn(length)
//        whenever(mimeTypeService.getMimeType(anyString())).thenReturn(aMimeType)
//
//        /* When */
//        val item = itemBusiness.addItemByUpload(idPodcast, uploadedFile)
//
//        /* Then */
//        assertThat(item.title).isEqualTo("aTitle")
//        assertThat(item.pubDate).isEqualTo(ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(title.split(" - ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1], DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.of(0, 0)), ZoneId.systemDefault()))
//        assertThat(item.url).isEqualTo(null)
//        assertThat(item.length).isEqualTo(length)
//        assertThat(item.mimeType).isEqualTo(aMimeType)
//        assertThat(item.description).isEqualTo("aDescription")
//        assertThat(item.fileName).isEqualTo(title)
//        assertThat(item.podcast).isEqualTo(podcast)
//        assertThat(item.status).isEqualTo(Status.FINISH)
//
//        assertThat(podcast.items).contains(item)
//        verify(podcastServerParameters, times(1)).rootfolder
//        verify(mimeTypeService, times(1)).getMimeType("mp3")
//        verify(podcastBusiness, times(1)).findOne(idPodcast)
//        verify(podcastBusiness, times(1)).save(podcast)
//        verify(itemRepository, times(1)).save(item)
//    }

    companion object {
        private const val ROOT_FOLDER = "/tmp/podcast"
    }
}
