package com.github.davinkevin.podcastserver.entity

import io.vavr.collection.HashSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.util.FileSystemUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Created by kevin on 15/06/15 for HackerRank problem
 */
class PodcastTest {
    private lateinit var podcast: Podcast
    private lateinit var anId: UUID

    @BeforeEach
    fun init() {
        Podcast.rootFolder = Paths.get("/tmp")

        /* Given */
        anId = UUID.randomUUID()
        podcast = Podcast().apply {
            id = anId
            title = "PodcastDeTest"
            url = "http://nowhere.com"
            signature = "ae4b93a7e8249d6be591649c936dbe7d"
            type = "Youtube"
            lastUpdate = NOW
            cover = COVER
            description = "A long Description"
            hasToBeDeleted = true
            items = HashSet.empty<Item>().toJavaSet()
            tags = HashSet.empty<Tag>().toJavaSet()
        }

        PODCAST_TO_STRING = "Podcast{id=$anId, title='PodcastDeTest', url='http://nowhere.com', signature='ae4b93a7e8249d6be591649c936dbe7d', type='Youtube', lastUpdate=%s}"

        FileSystemUtils.deleteRecursively(Podcast.rootFolder!!.resolve(podcast.title).toFile())
    }

    @Test
    fun should_have_all_setters_and_getters_working() {
        /* Then */
        assertThat(podcast.id).isEqualTo(anId)
        assertThat(podcast.title).isEqualTo("PodcastDeTest")
        assertThat(podcast.url).isEqualTo("http://nowhere.com")
        assertThat(podcast.signature).isEqualTo("ae4b93a7e8249d6be591649c936dbe7d")
        assertThat(podcast.type).isEqualTo("Youtube")
        assertThat(podcast.lastUpdate).isEqualTo(NOW)
        assertThat(podcast.cover).isEqualTo(COVER)
        assertThat(podcast.description).isEqualTo("A long Description")
        assertThat(podcast.hasToBeDeleted).isEqualTo(true)
        assertThat(podcast.items).isEmpty()
        assertThat(podcast.tags).isEmpty()
    }

    @Test
    fun should_have_toString() {
        assertThat(podcast.toString())
                .isEqualTo(String.format(PODCAST_TO_STRING, NOW))
    }

    @Test
    fun should_have_hashcode_and_equals() {
        /* Given */
        val samePodcast = Podcast()
        samePodcast.id = anId
        samePodcast.lastUpdate = NOW
        samePodcast.signature = "ae4b93a7e8249d6be591649c936dbe7d"
        samePodcast.title = "PodcastDeTest"
        samePodcast.url = "http://nowhere.com"

        val notPodcast = Any()

        /* Then */
        assertThat(podcast).isEqualTo(podcast)
        assertThat(podcast).isNotEqualTo(notPodcast)
        assertThat(podcast).isEqualTo(samePodcast)
        assertThat(podcast.hashCode()).isNotNull()
    }

    @Test
    fun should_add_an_item() {
        val itemToAdd = Item()
        /* When */
        podcast.add(itemToAdd)
        /* Then */
        assertThat(itemToAdd.podcast).isEqualTo(podcast)
        assertThat(podcast.items).containsOnly(itemToAdd)
    }

    @Test
    fun should_contains_item() {
        val itemToAdd = Item().apply { id = UUID.randomUUID() }
        val itemToAdd2 = Item().apply { id = UUID.randomUUID() }
        podcast.add(itemToAdd)

        assertThat(podcast.contains(itemToAdd)).isTrue()
        assertThat(podcast.contains(itemToAdd2)).isFalse()
    }


    @Test
    fun should_provide_cover_path() {
        /* GIVEN */
        /* WHEN  */
        val coverPath = podcast.coverPath

        /* THEN  */
        assertThat(coverPath).contains(Podcast.rootFolder!!.resolve(podcast.title).resolve("cover.jpg"))
    }

    companion object {

        private val NOW = ZonedDateTime.now()
        private val COVER = object : Cover() {
            init {
                url = "ACover.jpg"
            }
        }
        private lateinit var PODCAST_TO_STRING: String
    }

}
