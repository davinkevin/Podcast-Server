package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation
import com.github.davinkevin.podcastserver.cover.DeleteCoverInformation.*
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.item.CoverForItem
import com.github.davinkevin.podcastserver.item.DeleteItemInformation
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.item.PodcastForItem
import com.github.davinkevin.podcastserver.podcast.CoverForPodcast
import com.github.davinkevin.podcastserver.podcast.Podcast
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.tag.Tag
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.apache.commons.io.FilenameUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage.withPercentage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.FileSystemUtils
import reactor.test.StepVerifier
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Created by kevin on 2019-02-12
 */
@ExtendWith(SpringExtension::class)
@Import(FileService::class)
@ImportAutoConfiguration(WebClientAutoConfiguration::class)
class FileServiceTest(
    @Autowired val fileService: FileService
) {

    @MockBean private lateinit var p: PodcastServerParameters
    @MockBean private lateinit var mimeTypeService: MimeTypeService

    private val tempFolder = Paths.get("/tmp", "podcast-server-testing-folder", "FileService")

    @BeforeEach
    fun beforeEach() {
        Files.createDirectories(tempFolder)
        whenever(p.rootfolder).thenReturn(tempFolder)
    }

    @AfterEach
    fun afterEach() {
        FileSystemUtils.deleteRecursively(tempFolder)
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
                    description = "desc",
                    signature = null,
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

    @Nested
    @DisplayName("should download cover")
    inner class ShouldDownloadCover {

        private val wireMockServer: WireMockServer = WireMockServer(wireMockConfig().port(8089))

        @BeforeEach
        fun beforeEach() = wireMockServer.start()

        @AfterEach
        fun afterEach() = wireMockServer.stop()

        val podcast = Podcast(
                id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
                title = "Podcast title",
                description = "desc",
                signature = null,
                url = "https://foo.bar.com/app/file.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = CoverForPodcast(
                        id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                        url = URI("http://localhost:8089/img/image.png"),
                        height = 200, width = 200
                )
        )

        @Test
        fun `and save it to file`() {
            /* Given */
            /* When */
            StepVerifier.create(fileService.downloadPodcastCover(podcast))
                    /* Then */
                    .expectSubscription()
                    .verifyComplete()

            val file = tempFolder.resolve(podcast.title).resolve("${podcast.id}.${podcast.cover.extension()}")
            assertThat(file).exists()
            assertThat(file).hasDigest("MD5", "1cc21d3dce8bfedbda2d867a3238e8db")
        }
    }

    @Nested
    @DisplayName("should delegate the mimeType calls")
    inner class ShouldDelegateTheMimeTypeCalls {

        @Test
        fun `with success`() {
            /* Given */
            val file = Paths.get("/foo/bar/file.mp3")
            whenever(mimeTypeService.probeContentType(file)).thenReturn("audio/mp3")
            /* When */
            StepVerifier.create(fileService.probeContentType(file))
                    /* Then */
                    .expectSubscription()
                    .expectNext("audio/mp3")
                    .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should get size of a file")
    inner class ShouldGetSizeOfAFile {
        @Test
        fun `with success`(@TempDir dir: Path) {
            /* Given */
            val file = Files.createTempFile(dir, "foo", "bar")
            Files.write(file, "foo".toByteArray())
            /* When */
            StepVerifier.create(fileService.size(file))
            /* Then */
                    .expectSubscription()
                    .assertNext { assertThat(it).isCloseTo(3, withPercentage(1.toDouble())) }
                    .verifyComplete()
        }
    }

    @Nested
    @DisplayName("should delete cover")
    inner class ShouldDeleteCover {

        private val podcastName = "podcastCoverToDelete"
        private val podcastPath = tempFolder.resolve(podcastName)

        @BeforeEach
        fun beforeEach() {
            Files.createDirectory(tempFolder.resolve(podcastName))
        }

        @Test
        fun `when cover exists`() {
            /* Given */
            val itemId = UUID.randomUUID()
            Files.createFile(podcastPath.resolve("$itemId.png"))
            /* When */
            StepVerifier.create(fileService.deleteCover(DeleteCoverInformation(
                    UUID.randomUUID(), "png",
                    ItemInformation(itemId, "foo"),
                    PodcastInformation(UUID.randomUUID(), podcastName)
            )))
                    /* Then */
                    .expectSubscription()
                    .expectNext(true)
                    .verifyComplete()
        }

        @Test
        fun `when cover does not exist`() {
            /* Given */
            /* When */
            StepVerifier.create(fileService.deleteCover(DeleteCoverInformation(
                    UUID.randomUUID(), "png",
                    ItemInformation(UUID.randomUUID(), "foo"),
                    PodcastInformation(UUID.randomUUID(), podcastName)
            )))
                    /* Then */
                    .expectSubscription()
                    .expectNext(false)
                    .verifyComplete()
        }

    }
}

private fun CoverForPodcast.extension() = FilenameUtils.getExtension(url.toASCIIString()) ?: "jpg"
