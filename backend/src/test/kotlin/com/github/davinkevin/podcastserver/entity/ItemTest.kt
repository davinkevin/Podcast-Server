package com.github.davinkevin.podcastserver.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.util.FileSystemUtils
import java.io.IOException
import java.lang.Boolean.TRUE
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by kevin on 15/06/15 for HackerRank problem
 */
class ItemTest {

    @BeforeEach
    fun beforeEach() {
        /* Given */
        PODCAST = Podcast()
        PODCAST.title = "Fake Podcast"
        PODCAST.id = PODCAST_ID
        PODCAST.type = "Youtube"
        PODCAST.cover = PODCAST_COVER
        PODCAST.hasToBeDeleted = TRUE


        ITEM = Item()
        ITEM.id = ID
        ITEM.title = "Fake Item"
        ITEM.url = "http://fakeItem.com"
        ITEM.podcast = PODCAST
        ITEM.pubDate = NOW
        ITEM.description = "Fake item description"
        ITEM.mimeType = "video/mp4"
        ITEM.length = 123456L
        ITEM.cover = COVER
        ITEM.fileName = "fakeItem.mp4"

        Item.rootFolder = Paths.get("/tmp/podcast")

        FileSystemUtils.deleteRecursively(Item.rootFolder!!.toFile())
        Files.createDirectories(ITEM.localPath.parent)
        Files.createFile(ITEM.localPath)
    }

    @Test
    fun `should have be initialazed`() {
        /* When */
        /* Then */
        assertThat(ITEM.id).isEqualTo(ID)
        assertThat(ITEM.title).isEqualTo("Fake Item")
        assertThat(ITEM.url).isEqualTo("http://fakeItem.com")
        assertThat(ITEM.podcast).isEqualTo(PODCAST)
        assertThat(ITEM.pubDate).isEqualTo(NOW)
        assertThat(ITEM.description).isEqualTo("Fake item description")
        assertThat(ITEM.mimeType).isEqualTo("video/mp4")
        assertThat(ITEM.length).isEqualTo(123456L)
        assertThat(ITEM.cover).isEqualTo(COVER)
        assertThat(ITEM.fileName).isEqualTo("fakeItem.mp4")
        assertThat(ITEM.status).isEqualTo(Status.NOT_DOWNLOADED)
        assertThat(ITEM.progression).isEqualTo(0)
    }

    @Test
    fun `should change his status`() {

        /* When */ ITEM.status = Status.PAUSED
        /* Then */ assertThat(ITEM.status).isEqualTo(Status.PAUSED)

        /* When */ ITEM.status = Status.FINISH
        /* Then */ assertThat(ITEM.status).isEqualTo(Status.FINISH)
    }

    @Test
    fun `should advance in progression`() {
        /* When */ ITEM.progression = 50
        /* Then */ assertThat(ITEM.progression!!).isEqualTo(50)
    }

    @Test
    fun `should set the downloaddate`() {
        /* Given */
        val downloaddate = ZonedDateTime.now()
        /* When */  ITEM.downloadDate = downloaddate
        /* Then */  assertThat(ITEM.downloadDate).isEqualTo(downloaddate)
    }

    @Test
    fun `should_increment_the number of retry`() {
        assertThat(ITEM.numberOfFail).isEqualTo(0)

        ITEM.numberOfFail = 6
        assertThat(ITEM.numberOfFail!!).isEqualTo(6)

        ITEM.addATry()
        assertThat(ITEM.numberOfFail!!).isEqualTo(7)
    }

    @Test
    fun `should_have a valid url`() {
        assertThat(ITEM.hasValidURL()).isTrue()

        PODCAST.type = "upload"
        ITEM.url = ""
        assertThat(ITEM.hasValidURL()).isTrue()

        PODCAST.type = "Youtube"
        ITEM.url = ""
        assertThat(ITEM.hasValidURL()).isFalse()

    }

    @Test
    fun `should_report parent podcast id`() {
        assertThat(ITEM.podcastId).isEqualTo(PODCAST.id)

        ITEM.podcast = null
        assertThat(ITEM.podcastId)
                .isNotEqualTo(PODCAST.id)
                .isNull()
    }

    @Test
    @Disabled("will be fully removed when Item entity will be removed")
    fun `should_report parent podcast cover`() {
        val cover = Cover()
        cover.url = "/api/podcasts/$PODCAST_ID/items/$ID/cover.png"

        assertThat(ITEM.coverOfItemOrPodcast)
                .isEqualTo(cover)

        ITEM.cover = null
        assertThat(ITEM.coverOfItemOrPodcast).isSameAs(PODCAST_COVER)
    }

    @Test
    fun `should_expose the API url`() {
        assertThat(ITEM.proxyURLWithoutExtention)
                .isEqualTo(String.format("/api/v1/podcasts/%s/items/%s/" + "Fake_Item", PODCAST_ID, ID))

        assertThat(ITEM.proxyURL)
                .isEqualTo(String.format("/api/v1/podcasts/%s/items/%s/Fake_Item.mp4", PODCAST_ID, ID))
    }

    @Test
    fun `should_get the local path`() {
        /* When */
        val localPath = ITEM.localPath
        /* Then */ assertThat(localPath).hasFileName("fakeItem.mp4")

        /* When */
        val localStringPath = ITEM.localUri
        /* Then */ assertThat(localStringPath).isEqualTo("/tmp/podcast/Fake Podcast/fakeItem.mp4")
    }

    @Test
    fun `should expose isDownloaded`() {
        assertThat(ITEM.isDownloaded).isTrue()

        ITEM.fileName = null
        assertThat(ITEM.isDownloaded).isFalse()
    }

    @Test
    fun `should_change the local uri`() {
        /* When */
        ITEM.localUri = "http://www.google.fr/mavideo.mp4"
        /* Then */
        assertThat(ITEM.fileName).isEqualTo("mavideo.mp4")
    }

    @Test
    fun `should equals and hashcode`() {
        /* Given */
        val anObject = Any()
        val withSameId = Item()
        withSameId.id = ID
        val withSameUrl = Item()
        withSameUrl.url = ITEM.url
        val withSameName = Item()
        withSameName.url = "http://test.domain.com/toto/fakeItem.com"
        /*Item withSameLocalUri = new Item().setPodcast(PODCAST).setFileName("fakeItem.mp4");*/

        /* Then */
        assertThat(ITEM)
                .isEqualTo(ITEM)
                .isNotEqualTo(anObject)
                .isEqualTo(withSameId)
                .isEqualTo(withSameUrl)
                .isNotEqualTo(withSameName)/*.isEqualTo(withSameLocalUri)*/

        val expected = Item()
        expected.url = ITEM.url
        expected.pubDate = NOW
        assertThat(ITEM.hashCode()).isEqualTo(expected.hashCode())

    }

    @Test
    fun `should toString`() {
        assertThat(ITEM.toString())
                .isEqualTo("Item{id=$ID, title='Fake Item', url='http://fakeItem.com', pubDate=$NOW, description='Fake item description', mimeType='video/mp4', length=123456, status='NOT_DOWNLOADED', progression=0, downloaddate=null, podcast=Podcast{id=$PODCAST_ID, title='Fake Podcast', url='null', signature='null', type='Youtube', lastUpdate=null}, numberOfTry=0}")

    }

    @Test
    fun `should delete`() {
        /* Given */
        ITEM.status = Status.FINISH
        /* When  */
        ITEM.deleteDownloadedFile()
        /* Then  */
        assertThat(ITEM.fileName).isEqualTo(null)
        assertThat(ITEM.status).isEqualTo(Status.DELETED)
    }

    @Test
    @Throws(IOException::class)
    fun `should preremove`() {
        /* Given */
        val fileToDelete = ITEM.localPath

        /* When */
        ITEM.preRemove()

        /* Then */
        assertThat(fileToDelete).doesNotExist()
    }

    @Test
    fun `should_generate exception on deletion`() {
        /* Given */
        Item.rootFolder = Paths.get("/")
        PODCAST.title = "sbin"
        ITEM.fileName = "fsck"

        /* When */  ITEM.deleteDownloadedFile()
    }

    companion object {

        private val PODCAST_ID = UUID.randomUUID()
        private var ITEM = Item()
        private var PODCAST: Podcast = object : Podcast() {
            init {
                id = UUID.randomUUID()
            }
        }
        private val NOW = ZonedDateTime.now()
        private val COVER = object : Cover() {
            init {
                url = "http://fakeItem.com/cover.png"
            }
        }
        private val PODCAST_COVER = object : Cover() {
            init {
                url = "PodcastCover"
            }
        }
        private val ID = UUID.randomUUID()
    }

}
