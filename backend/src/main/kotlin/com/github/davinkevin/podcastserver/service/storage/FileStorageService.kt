package com.github.davinkevin.podcastserver.service.storage

import com.github.davinkevin.podcastserver.cover.Cover
import com.github.davinkevin.podcastserver.cover.DeleteCoverRequest
import com.github.davinkevin.podcastserver.item.DeleteItemRequest
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.podcast.DeletePodcastRequest
import com.github.davinkevin.podcastserver.podcast.Podcast
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.random.Random

/**
 * Created by kevin on 2019-02-09
 */
class FileStorageService(
    private val rcb: RestClient.Builder,
    private val bucket: S3AsyncClient,
    private val preSignerBuilder: (URI) -> S3Presigner,
    private val properties: StorageProperties,
) {

    private val log = LoggerFactory.getLogger(FileStorageService::class.java)

    fun deletePodcast(podcast: DeletePodcastRequest): Boolean {
        log.info("Deletion of podcast {}", podcast.title)

        val result = bucket.listObjects { it.bucket(properties.bucket).prefix(podcast.title) }
            .runCatching { join() }
            .getOrNull() ?: return false

        return result
            .contents()
            .map { bucket.deleteObject(it.toDeleteRequest()) }
            .map { it.runCatching { join() } }
            .all { it.isSuccess }
    }

    fun deleteItem(item: DeleteItemRequest): Boolean {
        val path = "${item.podcastTitle}/${item.fileName}"

        log.info("Deletion of file {}", path)

        return bucket.deleteObject { it.bucket(properties.bucket).key(path) }
            .runCatching { join() }
            .isSuccess
    }

    fun deleteCover(cover: DeleteCoverRequest): Boolean {
        val path = "${cover.podcast.title}/${cover.item.id}.${cover.extension}"

        log.info("Deletion of file {}", path)

        return bucket.deleteObject { it.bucket(properties.bucket).key(path) }
            .runCatching { join() }
            .isSuccess
    }

    fun coverExists(p: Podcast) = coverExists(p.title, p.id, p.cover.extension())
    fun coverExists(i: Item) = coverExists(i.podcast.title, i.id, i.cover.extension())
    fun coverExists(podcastTitle: String, itemId: UUID, extension: String): Path? {
        val path = "$podcastTitle/$itemId.$extension"

        val result = bucket.headObject { it.bucket(properties.bucket).key(path) }
            .runCatching { join() }

        if (result.isFailure) {
            return null
        }

        return path.substringAfterLast("/")
            .let(::Path)
    }

    private fun download(url: URI): ByteArrayResource? = rcb.clone()
        .baseUrl(url.toASCIIString())
        .build()
        .get()
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .retrieve()
        .body<ByteArrayResource>()

    private fun upload(key: String, resource: ByteArrayResource): PutObjectResponse? {
        val request = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .key(key)
            .build()

        return bucket.putObject(request, AsyncRequestBody.fromBytes(resource.byteArray))
            .runCatching { join() }
            .getOrNull()
    }

    fun downloadPodcastCover(podcast: Podcast) {
        val response = download(podcast.cover.url)
            ?: return

        upload("""${podcast.title}/${podcast.id}.${podcast.cover.extension()}""", response)
    }

    fun downloadItemCover(item: Item) {
        val response = download(item.cover.url)
            ?: return

        upload("""${item.podcast.title}/${item.id}.${item.cover.extension()}""", response)
    }

    fun movePodcast(request: MovePodcastRequest) {
        val listRequest = ListObjectsRequest.builder()
            .bucket(properties.bucket)
            .prefix(request.from)
            .build()

        val result = bucket.listObjects(listRequest)
            .runCatching { join() }
            .getOrNull() ?: return

        result.contents().forEach {
            val copy = it.toCopyRequest(request.to)
            val delete = it.toDeleteRequest()

            bucket.copyObject(copy).runCatching { join() }
                .onFailure { t -> log.error("Error during copy of ${it.key()}", t) }
                .getOrNull() ?: return@forEach

            bucket.deleteObject(delete).runCatching { join() }
                .onFailure { t -> log.error("Error during deletion of ${it.key()}", t) }
                .getOrNull() ?: return@forEach
        }
    }

    fun upload(podcastTitle: String, file: Path): PutObjectResponse? {
        val request = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .key("$podcastTitle/${file.fileName}")
            .build()

        val response = retry { bucket.putObject(request, file).join() }
            .onFailure { log.error("Error during upload of file ${file.fileName} to ${request.key()}", it) }
            .getOrNull()

        Files.deleteIfExists(file)

        return response
    }

    fun upload(request: UploadFromStreamRequest) {
        val (podcastTitle, fileName, stream) = request

        val putRequest = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .key("$podcastTitle/${fileName.fileName}")
            .build()

        val body = AsyncRequestBody.fromInputStream { it
            .inputStream(stream)
            .executor(Executors.newVirtualThreadPerTaskExecutor())
        }

        retry { bucket.putObject(putRequest, body).join() }
    }

    data class UploadFromStreamRequest(val podcastTitle: String, val fileName: Path, val stream: InputStream)

    fun <T> retry(block: () -> T): Result<T> {
        val retries = 3
        val delay = Duration.ofSeconds(1)
        val errors = mutableListOf<Result<T>>()

        for (i in 1..retries) {
            val result = runCatching { block() }
            if (result.isSuccess) return result
            errors += result

            val waitTime = delay.plusSeconds(Random.nextDouble(0.0, 0.5).toLong())
            Thread.sleep(waitTime)
        }

        return errors.first()
    }

    fun metadata(title: String, file: Path): FileMetaData? {
        val key = "$title/${file.fileName}"

        val result = retry { bucket.headObject { it.bucket(properties.bucket).key(key) }.join() }
            .getOrNull()
            ?: return null

        return FileMetaData(contentType = result.contentType(), size = result.contentLength())
    }

    private fun S3Object.toDeleteRequest(bucket: String = properties.bucket): DeleteObjectRequest =
        DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(this.key())
            .build()

    private fun S3Object.toCopyRequest(key: String, bucket: String = properties.bucket): CopyObjectRequest =
        CopyObjectRequest.builder()
            .sourceBucket(bucket)
            .sourceKey(this.key())
            .destinationBucket(bucket)
            .destinationKey("$key/" + this.key().substringAfterLast("/"))
            .build()

    fun initBucket() {
        val result = bucket.headBucket { it.bucket(properties.bucket) }
            .runCatching { join() }

        if (result.isSuccess) {
            log.info("🗂 Bucket already present")
            return
        }

        bucket.createBucket { it.bucket(properties.bucket) }.join()
        log.info("✅ Bucket creation done")
    }

    fun toExternalUrl(file: FileDescriptor, requestedHost: URI): URI {
        return preSignerBuilder(requestedHost).presignGetObject { sign -> sign
            .signatureDuration(Duration.ofDays(1))
            .getObjectRequest { request -> request
                .bucket(properties.bucket)
                .key("${file.podcastTitle}/${file.fileName}")
            }
        }.url().toURI()
    }
}

private fun Cover.extension(): String = Path(url.path).extension.ifBlank { "jpg" }
private fun Item.Cover.extension() = Path(url.path).extension.ifBlank { "jpg" }

data class MovePodcastRequest(val id: UUID, val from: String, val to: String)
data class FileMetaData(val contentType: String, val size: Long)
data class FileDescriptor(val podcastTitle: String, val fileName: Path)
