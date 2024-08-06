package com.github.davinkevin.podcastserver.service.storage

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.cover.DeleteCoverRequest
import com.github.davinkevin.podcastserver.cover.DeleteCoverRequest.*
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.fileAsByteArray
import com.github.davinkevin.podcastserver.item.DeleteItemRequest
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.podcast.DeletePodcastRequest
import com.github.davinkevin.podcastserver.podcast.Podcast
import com.github.davinkevin.podcastserver.tag.Tag
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestClient
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

/**
 * Created by kevin on 2019-02-12
 */
const val s3MockBackendPort = 1234

@Suppress("HttpUrlsUsage")
@Import(FileStorageConfig::class)
@TestPropertySource(properties = [
    "podcastserver.storage.bucket=data",
    "podcastserver.storage.username=foo",
    "podcastserver.storage.password=bar",
    "podcastserver.storage.url=http://localhost:$s3MockBackendPort/",
])
@ExtendWith(SpringExtension::class)
@ImportAutoConfiguration(RestClientAutoConfiguration::class)
class FileStorageServiceTest(
    @Autowired val fileService: FileStorageService
) {

    @Nested
    @DisplayName("should delete podcast")
    inner class ShouldDeletePodcast {

        val request = DeletePodcastRequest(id = UUID.randomUUID(), title = "podcast-title")

        @Test
        fun `with success`() {
            /* Given */
            s3Backend.stubFor(get("/data?prefix=podcast-title").willReturn(okXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                    <Name>data</Name>
                    <IsTruncated>false</IsTruncated>
                    <Contents>
                        <Key>podcast-title/first.mp3</Key>
                    </Contents>
                    <Contents>
                       <Key>podcast-title/second.mp3</Key>
                    </Contents>
                </ListBucketResult>
            """.trimIndent())))
            s3Backend.stubFor(delete("/data/podcast-title/first.mp3").willReturn(ok()))
            s3Backend.stubFor(delete("/data/podcast-title/second.mp3").willReturn(ok()))

            /* When */
            val result = fileService.deletePodcast(request)

            /* Then */
            assertThat(result).isTrue()
        }

        @Test
        fun `with error because one item can't be deleted`() {
            /* Given */
            s3Backend.stubFor(get("/data?prefix=podcast-title").willReturn(okXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                    <Name>data</Name>
                    <IsTruncated>false</IsTruncated>
                    <Contents>
                        <Key>podcast-title/first.mp3</Key>
                    </Contents>
                    <Contents>
                       <Key>podcast-title/second.mp3</Key>
                    </Contents>
                </ListBucketResult>
            """.trimIndent())))
            s3Backend.stubFor(delete("/data/podcast-title/first.mp3").willReturn(ok()))
            s3Backend.stubFor(delete("/data/podcast-title/second.mp3").willReturn(notFound()))

            /* When */
            val result = fileService.deletePodcast(request)

            /* Then */
            assertThat(result).isFalse()
        }

        @Test
        fun `with error because podcast is not present`() {
            /* Given */
            s3Backend.stubFor(get("/data?prefix=podcast-title").willReturn(notFound()))

            /* When */
            val result = fileService.deletePodcast(request)

            /* Then */
            assertThat(result).isFalse()
        }

    }

    @Nested
    @DisplayName("should delete item file")
    inner class ShouldDeleteItemFile {

        val request = DeleteItemRequest(UUID.randomUUID(), Path("foo.txt"), "podcast-title")

        @Test
        fun `with success`() {
            /* Given */
            s3Backend.stubFor(delete("/data/podcast-title/foo.txt").willReturn(ok()))

            /* When */
            val isDeleted = fileService.deleteItem(request)

            /* Then */
            assertThat(isDeleted).isTrue()
        }

        @Test
        fun `with error`() {
            /* Given */
            s3Backend.stubFor(delete("/data/podcast-title/foo.txt").willReturn(notFound()))

            /* When */
            val isDeleted = fileService.deleteItem(request)

            /* Then */
            assertThat(isDeleted).isFalse()
        }
    }

    @Nested
    @DisplayName("should delete cover")
    inner class ShouldDeleteCover {

        private val request = DeleteCoverRequest(
            UUID.randomUUID(), "png",
            Item(UUID.fromString("4ab252dc-1cf4-4f60-ba18-1d91d1917ee6"), "foo"),
            Podcast(UUID.randomUUID(), "podcast-title")
        )

        @Test
        fun `with success`() {
            /* Given */
            s3Backend.stubFor(delete("/data/podcast-title/4ab252dc-1cf4-4f60-ba18-1d91d1917ee6.png")
                .willReturn(ok()))

            /* When */
            val isDeleted = fileService.deleteCover(request)

            /* Then */
            assertThat(isDeleted).isTrue()
        }

        @Test
        fun `with error`() {
            /* Given */
            s3Backend.stubFor(delete("/data/podcast-title/4ab252dc-1cf4-4f60-ba18-1d91d1917ee6.png")
                .willReturn(notFound()))

            /* When */
            val isDeleted = fileService.deleteCover(request)

            /* Then */
            assertThat(isDeleted).isFalse()
        }
    }

    @Nested
    @DisplayName("should check if cover exists")
    inner class ShouldCheckIfCoverExists {

        @Nested
        @DisplayName("for Podcast")
        inner class ForPodcast {

            val podcast = Podcast(
                id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
                title = "podcast-title",
                description = "desc",
                signature = null,
                url = "https://foo.bar.com/app/file.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = Cover(
                    id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                    url = URI("https://external.domain.tld/cover.png"),
                    height = 200, width = 200
                )
            )

            @Test
            fun `and it exists`() {
                /* Given */
                s3Backend.stubFor(head(urlEqualTo("/data/podcast-title/dd16b2eb-657e-4064-b470-5b99397ce729.png"))
                    .willReturn(ok()))

                /* When */
                val coverPath = fileService.coverExists(podcast)

                /* Then */
                assertThat(coverPath).isEqualTo(Path("dd16b2eb-657e-4064-b470-5b99397ce729.png"))
            }

            @Test
            fun `and it exists with default extension`() {
                /* Given */
                val specificPodcast = podcast.copy(
                    cover = podcast.cover.copy(url = URI.create("https://external.domain.tld/cover"))
                )
                s3Backend.stubFor(head(urlEqualTo("/data/podcast-title/dd16b2eb-657e-4064-b470-5b99397ce729.jpg"))
                    .willReturn(ok()))

                /* When */
                val coverPath = fileService.coverExists(specificPodcast)

                /* Then */
                assertThat(coverPath).isEqualTo(Path("dd16b2eb-657e-4064-b470-5b99397ce729.jpg"))
            }

            @Test
            fun `and it doesn't exist`() {
                /* Given */
                s3Backend.stubFor(head(urlEqualTo("/data/podcast-title/dd16b2eb-657e-4064-b470-5b99397ce729.png"))
                    .willReturn(notFound()))

                /* When */
                val coverPath = fileService.coverExists(podcast)

                /* Then */
                assertThat(coverPath).isNull()
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
                mimeType = "audio/mp3",
                length = 100,
                fileName = null,
                status = Status.NOT_DOWNLOADED,

                podcast = Item.Podcast(
                    id = UUID.fromString("8e2df56f-959b-4eb4-b5fa-0fd6027ae0f9"),
                    title = "podcast-title",
                    url = "https://external.domain.tld/bar.rss"
                ),
                cover = Item.Cover(
                    id = UUID.fromString("f4efe8db-7abf-4998-b15c-9fa2e06096a1"),
                    url = URI("https://external.domain.tld/foo/bar.png"),
                    width = 200,
                    height = 200
                )
            )

            @Test
            fun `and it exists`() {
                /* Given */
                s3Backend.stubFor(head(urlEqualTo("/data/podcast-title/27184b1a-7642-4ffd-ac7e-14fb36f7f15c.png"))
                    .willReturn(ok()))

                /* When */
                val coverPath = fileService.coverExists(item)

                /* Then */
                assertThat(coverPath).isEqualTo(Path("27184b1a-7642-4ffd-ac7e-14fb36f7f15c.png"))
            }

            @Test
            fun `and it exists with default extension`() {
                /* Given */
                val specificItem = item.copy(
                    cover = item.cover.copy(
                        url = URI.create("https://external.domain.tld/foo/bar")
                    )
                )
                s3Backend.stubFor(head(urlEqualTo("/data/podcast-title/27184b1a-7642-4ffd-ac7e-14fb36f7f15c.jpg"))
                    .willReturn(ok()))

                /* When */
                val coverPath = fileService.coverExists(specificItem)

                /* Then */
                assertThat(coverPath).isEqualTo(Path("27184b1a-7642-4ffd-ac7e-14fb36f7f15c.jpg"))
            }

            @Test
            fun `and it doesn't exist`() {
                /* Given */
                s3Backend.stubFor(head(urlEqualTo("/data/podcast-title/27184b1a-7642-4ffd-ac7e-14fb36f7f15c.png"))
                    .willReturn(notFound()))

                /* When */
                val coverPath = fileService.coverExists(item)

                /* Then */
                assertThat(coverPath).isEqualTo(null)
            }
        }
    }

    @Nested
    @DisplayName("should download cover")
    inner class ShouldDownloadCover {

        @JvmField
        @RegisterExtension
        val externalBackend: WireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8089))
            .build()

        @Nested
        @DisplayName("for podcast")
        inner class ForPodcast {

            private val podcast = Podcast(
                id = UUID.fromString("dd16b2eb-657e-4064-b470-5b99397ce729"),
                title = "podcast-title",
                description = "desc",
                signature = null,
                url = "https://foo.bar.com/app/file.rss",
                hasToBeDeleted = true,
                lastUpdate = OffsetDateTime.of(2019, 3, 31, 11, 21, 32, 45, ZoneOffset.ofHours(1)),
                type = "RSS",
                tags = setOf(Tag(UUID.fromString("f9d92927-1c4c-47a5-965d-efbb2d422f0c"), "Cinéma")),

                cover = Cover(
                    id = UUID.fromString("1e275238-4cbe-4abb-bbca-95a0e4ebbeea"),
                    url = URI("http://localhost:8089/img/image.png"),
                    height = 200, width = 200
                )
            )

            @Test
            fun `and save it to file`() {
                /* Given */
                externalBackend.stubFor(get("/img/image.png").willReturn(ok().withBody(fileAsByteArray("/__files/img/image.png"))))
                s3Backend.stubFor(put("/data/podcast-title/dd16b2eb-657e-4064-b470-5b99397ce729.png").willReturn(ok()))

                /* When */
                fileService.downloadPodcastCover(podcast)

                /* Then */
                val bodyDigest = s3Backend.findAll(newRequestPattern(RequestMethod.PUT, urlEqualTo("/data/podcast-title/dd16b2eb-657e-4064-b470-5b99397ce729.png")))
                    .first().body
                    .let(DigestUtils::md5DigestAsHex)

                assertThat(bodyDigest).isEqualTo("1cc21d3dce8bfedbda2d867a3238e8db")
            }
        }

        @Nested
        @DisplayName("for item")
        inner class ForItem {

            private val item = Item(
                id = UUID.fromString("f47421c2-f7fd-480a-b266-635a97c301dd"),
                title = "Item title",
                url = "http://foo.bar.com/item.1.mp3",

                pubDate = null,
                downloadDate = null,
                creationDate = null,

                description = null,
                mimeType = "audio/mp3",
                length = null,
                fileName = Path("item.1.mp3"),
                status = Status.NOT_DOWNLOADED,

                podcast = Item.Podcast(
                    id = UUID.fromString("096fc02f-e3d8-46a0-a523-6a479c573c73"),
                    title = "podcast-title",
                    url = null
                ),
                cover = Item.Cover(
                    id = UUID.fromString("054b45d2-4f7a-4161-98ba-7050630ee000"),
                    url = URI("http://localhost:8089/img/image.png"),
                    width = 100,
                    height = 200
                )
            )

            @Test
            fun `and save it in podcast folder with specific name`() {
                /* Given */
                val urlInBucket = "/data/podcast-title/f47421c2-f7fd-480a-b266-635a97c301dd.png"
                externalBackend.stubFor(get("/img/image.png")
                    .willReturn(ok().withBody(fileAsByteArray("/__files/img/image.png"))))
                s3Backend.stubFor(put(urlInBucket).willReturn(ok()))

                /* When */
                fileService.downloadItemCover(item)

                /* Then */
                val bodyDigest = s3Backend.findAll(newRequestPattern(RequestMethod.PUT, urlEqualTo(urlInBucket)))
                    .first().body
                    .let(DigestUtils::md5DigestAsHex)

                assertThat(bodyDigest).isEqualTo("1cc21d3dce8bfedbda2d867a3238e8db")
            }

        }

    }

    @Nested
    @DisplayName("should move podcast")
    inner class ShouldMovePodcastDetails {

        private val moveOperation = MovePodcastRequest(
            id = UUID.fromString("1d677bae-f58a-48e4-91ad-c95745e86d31"),
            from = "origin",
            to = "destination"
        )

        @Test
        fun `with item in it`() {
            /* Given */
            s3Backend.apply {
                stubFor(get("/data?prefix=origin").willReturn(okXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                        <Name>data</Name>
                        <IsTruncated>false</IsTruncated>
                        <Contents>
                            <Key>origin/first.mp3</Key>
                        </Contents>
                        <Contents>
                           <Key>origin/second.mp3</Key>
                        </Contents>
                    </ListBucketResult>
                """.trimIndent())))
                stubFor(put("/data/destination/first.mp3")
                    .withHeader("x-amz-copy-source", equalTo("data/origin/first.mp3"))
                    .willReturn(ok()))
                stubFor(put("/data/destination/second.mp3")
                    .withHeader("x-amz-copy-source", equalTo("data/origin/second.mp3"))
                    .willReturn(ok()))
                stubFor(delete("/data/origin/first.mp3").willReturn(ok()))
                stubFor(delete("/data/origin/second.mp3").willReturn(ok()))
            }

            /* When */
            fileService.movePodcast(moveOperation)

            /* Then */
            s3Backend.apply {
                verify(putRequestedFor(urlEqualTo("/data/destination/first.mp3"))
                    .withHeader("x-amz-copy-source", equalTo("data/origin/first.mp3")))
                verify(putRequestedFor(urlEqualTo("/data/destination/second.mp3"))
                    .withHeader("x-amz-copy-source", equalTo("data/origin/second.mp3")))
                verify(deleteRequestedFor(urlEqualTo("/data/origin/first.mp3")))
                verify(deleteRequestedFor(urlEqualTo("/data/origin/second.mp3")))

            }
        }
    }

    @Nested
    @DisplayName("should upload Path")
    inner class ShouldUploadPath {

        @Test
        fun `with success`(@TempDir dir: Path) {
            /* Given */
            val file = dir.resolve("toUpload.txt").apply { writeText("text is here !") }
            s3Backend.stubFor(put("/data/podcast-title/toUpload.txt").willReturn(ok()))

            /* When */
            val result = fileService.upload("podcast-title", file)

            /* Then */
            assertThat(result).isNotNull()

            /* And */
            val allRequests = s3Backend.findAll(newRequestPattern(RequestMethod.PUT, urlEqualTo("/data/podcast-title/toUpload.txt")))
            assertThat(allRequests).hasSize(1)

            /* And */
            val textContent = allRequests.first().body.decodeToString()
            assertThat(textContent).isEqualTo("text is here !")
        }

        @Test
        fun `with success after second retry`(@TempDir dir: Path) {
            /* Given */
            val file = dir.resolve("toUpload.txt").apply { writeText("text is here !") }
            s3Backend.apply {
                stubFor(put("/data/podcast-title/toUpload.txt").willReturn(badRequest()))
                stubFor(put("/data/podcast-title/toUpload.txt").willReturn(ok()))
            }

            /* When */
            val result = fileService.upload("podcast-title", file)

            /* Then */
            assertThat(result).isNotNull()

            val textContent = s3Backend.findAll(newRequestPattern(RequestMethod.PUT, urlEqualTo("/data/podcast-title/toUpload.txt")))
                .first().body
                .decodeToString()

            assertThat(textContent).isEqualTo("text is here !")
        }

        @Test
        fun `with failure after all retries`(@TempDir dir: Path) {
            /* Given */
            val file = dir.resolve("toUpload.txt").apply { writeText("text is here !") }
            s3Backend.apply {
                stubFor(put("/data/podcast-title/toUpload.txt").willReturn(badRequest()))
                stubFor(put("/data/podcast-title/toUpload.txt").willReturn(badRequest()))
                stubFor(put("/data/podcast-title/toUpload.txt").willReturn(badRequest()))
                stubFor(put("/data/podcast-title/toUpload.txt").willReturn(badRequest()))
            }

            /* When */
            val result = fileService.upload("podcast-title", file)

            /* Then */
            assertThat(result).isNull()
        }
    }


    @Nested
    @DisplayName("should upload InputStream")
    inner class ShouldUploadInputStream {

        @Test
        fun `with success`(@TempDir dir: Path) {
            /* Given */
            val file = dir.resolve("toUpload.txt").also(Files::createFile)
            s3Backend.stubFor(put("/data/podcast-title/toUpload.txt").willReturn(ok()))

            /* And */
            val request = FileStorageService.UploadFromStreamRequest(
                podcastTitle = "podcast-title",
                fileName = file.fileName,
                stream = file.inputStream()
            )

            /* When */
            fileService.upload(request)

            /* Then */
            val wiremockRequest = s3Backend.findAll(newRequestPattern(RequestMethod.PUT, urlEqualTo("/data/podcast-title/toUpload.txt")))
            assertThat(wiremockRequest).isNotEmpty
        }

    }

    @Nested
    @DisplayName("should fetch metadata")
    inner class ShouldFetchMetadata {

        @Test
        fun `with success`() {
            /* Given */
            s3Backend.stubFor(head(urlEqualTo("/data/podcast-title/dd16b2eb-657e-4064-b470-5b99397ce729.png"))
                .willReturn(ok()
                    .withHeader("Content-Type", "image/png")
                    .withHeader("Content-Length", "123")
                ))
            /* When */
            val result = fileService.metadata("podcast-title", Paths.get("dd16b2eb-657e-4064-b470-5b99397ce729.png"))

            /* Then */
            assertThat(result).isEqualTo(FileMetaData(
                contentType = "image/png",
                size = 123L
            ))
        }

    }

    @Nested
    @DisplayName("should init bucket")
    inner class ShouldInitBucket {

        @Test
        fun `with no bucket before`() {
            /* Given */
            s3Backend.apply {
                stubFor(head(urlEqualTo("/data")).willReturn(notFound()))
                stubFor(put("/data").willReturn(ok()))
            }

            /* When */
            fileService.initBucket()

            /* Then */
            s3Backend.apply {
                verify(headRequestedFor(urlEqualTo("/data")))
                verify(putRequestedFor(urlEqualTo("/data")))
            }
        }

        @Test
        fun `with an already existing bucket`() {
            /* Given */
            s3Backend.stubFor(head(urlEqualTo("/data")).willReturn(ok()))

            /* When */
            fileService.initBucket()

            /* Then */
            s3Backend.apply {
                verify(headRequestedFor(urlEqualTo("/data")))
                verify(0, putRequestedFor(urlEqualTo("/data")))
            }
        }

    }

    @Nested
    @DisplayName("should sign url")
    inner class ShouldSignUrl {

        private val storageProperties = StorageProperties(
            bucket = "foo",
            username = "name",
            password = "pass",
            url = URI.create("https://storage.local/"),
            isInternal = true
        )

        @Test
        fun `with domain from user request`() {
            /* Given */
            val onDemandFileStorageService = FileStorageConfig().fileStorageService(
                RestClient.builder(),
                storageProperties.copy(isInternal = true)
            )

            /* When */
            val uri = onDemandFileStorageService.toExternalUrl(
                FileDescriptor("bar", Path("zoo")),
                URI.create("https://request.local/")
            )

            /* Then */
            assertThat(uri.host).isEqualTo("request.local")
        }

        @Test
        fun `with domain from external storage system`() {
            /* Given */
            val onDemandFileStorageService = FileStorageConfig().fileStorageService(
                RestClient.builder(),
                storageProperties.copy(isInternal = false)
            )

            /* When */
            val uri = onDemandFileStorageService.toExternalUrl(
                FileDescriptor("bar", Path("zoo")),
                URI.create("https://request.local/")
            )

            /* Then */
            assertThat(uri.host).isEqualTo("storage.local")
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val s3Backend: WireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig()
                .port(s3MockBackendPort)
                //.notifier(ConsoleNotifier(true))
            )
            .build()
    }
}
