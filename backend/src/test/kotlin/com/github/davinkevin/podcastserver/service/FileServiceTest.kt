package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.item.CoverForItem
import com.github.davinkevin.podcastserver.item.DeleteItemInformation
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.item.PodcastForItem
import com.github.davinkevin.podcastserver.podcast.CoverForPodcast
import com.github.davinkevin.podcastserver.podcast.Podcast
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.tag.Tag
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.FileSystemUtils
import reactor.core.publisher.Hooks
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Created by kevin on 2019-02-12
 */
@ExtendWith(SpringExtension::class)
@Import(FileService::class)
class FileServiceTest {

    @Autowired lateinit var fileService: FileService
    @MockBean lateinit var p: PodcastServerParameters

    private val tempFolder = Paths.get("/tmp", "podcast-server", "FileService")

    @BeforeEach
    fun beforeEach() {
        Files.createDirectories(tempFolder)
        whenever(p.rootfolder).thenReturn(tempFolder)
    }

    @AfterEach
    fun afterEach() {
        FileSystemUtils.deleteRecursively(tempFolder.toFile())
    }

    @Test
    fun `should delete file relatively`() {
        /* Given */
        val dii = DeleteItemInformation(UUID.randomUUID(), "foo.txt", "podcast-title")
        val fileCreate = tempFolder.resolve(dii.path)
        Files.createDirectories(fileCreate.parent)
        val f = Files.createFile(fileCreate)

        /* When */
        StepVerifier.create(fileService.deleteItem(dii))
                .expectSubscription()
                /* Then */
                .expectNext(true)
                .then {
                    assertThat(Files.exists(f)).isFalse()
                }
                .verifyComplete()
    }

    @Nested
    @DisplayName("should check if cover exists")
    inner class ShouldCheckIfCoverExists {

        @Nested
        @DisplayName("for Podcast")
        inner class ForPodcast {

            val podcast = Podcast(
                    id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
                    title = "Podcast title",
                    url = "https://foo.bar.com/app/file.rss",
                    hasToBeDeleted = true,
                    lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                    type = "RSS",
                    tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                    cover = CoverForPodcast(
                            id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                            url = URI("https://external.domain.tld/cover.png"),
                            height = 200, width = 200
                    )
            )

            @Test
            fun `and it exists`() {
                /* Given */
                val podcastFolder = Files.createDirectory(tempFolder.resolve(podcast.title))
                Files.createFile(podcastFolder.resolve("dd16b2eb-657e-4064-b470-5b99397ce729.png"))

                /* When */
                StepVerifier.create(fileService.coverExists(podcast))
                        .expectSubscription()
                        /* Then */
                        .expectNext("dd16b2eb-657e-4064-b470-5b99397ce729.png")
                        .verifyComplete()
            }

            @Test
            fun `and it doesn't exist`() {
                /* Given */
                /* When */
                StepVerifier.create(fileService.coverExists(podcast))
                        .expectSubscription()
                        /* Then */
                        .verifyComplete()
            }

        }

        @Nested
        @DisplayName("for Item")
        inner class ForItem {

            val item = Item(
                    id = UUID.fromString("27184b1a-7642-4ffd-ac7e-14fb36f7f15c"),
                    title = "Foo",
                    url = "https://external.domain.tld/foo/bar.mp4",

                    pubDate = OffsetDateTime.now(),
                    downloadDate = OffsetDateTime.now(),
                    creationDate = OffsetDateTime.now(),

                    description = "desc",
                    mimeType = null,
                    length = 100,
                    fileName = null,
                    status = Status.NOT_DOWNLOADED,

                    podcast = PodcastForItem(
                            id = UUID.fromString("8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9"),
                            title = "Podcast Bar",
                            url = "https://external.domain.tld/bar.rss"
                    ),
                    cover = CoverForItem(
                            id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
                            url = "https://external.domain.tld/foo/bar.png",
                            width = 200,
                            height = 200
                    )
            )

            @Test
            fun `and it exists`() {
                /* Given */
                val podcastFolder = Files.createDirectory(tempFolder.resolve(item.podcast.title))
                Files.createFile(podcastFolder.resolve("27184b1a-7642-4ffd-ac7e-14fb36f7f15c.png"))

                /* When */
                StepVerifier.create(fileService.coverExists(item))
                        .expectSubscription()
                        /* Then */
                        .expectNext("27184b1a-7642-4ffd-ac7e-14fb36f7f15c.png")
                        .verifyComplete()
            }

            @Test
            fun `and it doesn't exist`() {
                /* Given */
                /* When */
                StepVerifier.create(fileService.coverExists(item))
                        .expectSubscription()
                        /* Then */
                        .verifyComplete()
            }

        }

    }
}
