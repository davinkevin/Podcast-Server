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
        }

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
    }

}
