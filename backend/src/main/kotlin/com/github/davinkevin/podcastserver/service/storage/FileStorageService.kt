package com.github.davinkevin.podcastserver.service.storage

import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
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
): CoverExists, ToExternalUrl, DeleteObject, Upload {

    private val log = LoggerFactory.getLogger(FileStorageService::class.java)

    override fun delete(request: DeleteRequest): Boolean {
        return when(request) {
            is DeleteRequest.ForItem -> deleteObject("${request.podcastTitle}/${request.fileName}")
            is DeleteRequest.ForCover -> deleteObject("${request.podcast.title}/${request.item.id}.${request.extension}")
            is DeleteRequest.ForPodcast -> deleteAllInside(request.title)
        }
    }

    private fun deleteObject(path: String): Boolean {
        return bucket.deleteObject { it.bucket(properties.bucket).key(path) }
            .runCatching { join() }
            .isSuccess
    }

    private fun deleteAllInside(path: String): Boolean {
        val result = bucket.listObjects { it.bucket(properties.bucket).prefix(path) }
            .runCatching { join() }
            .getOrNull() ?: return false

        return result
            .contents()
            .map { bucket.deleteObject(it.toDeleteRequest()) }
            .map { it.runCatching { join() } }
            .all { it.isSuccess }
    }

    override fun coverExists(r: CoverExistsRequest): Path? {
        val path = when(r) {
            is CoverExistsRequest.ForPlaylist -> ".playlist/${r.name}/${r.id}.${r.coverExtension}"
            is CoverExistsRequest.ForPodcast -> "${r.title}/${r.id}.${r.coverExtension}"
            is CoverExistsRequest.ForItem -> "${r.podcastTitle}/${r.id}.${r.coverExtension}"
        }

        val result = bucket.headObject { it.bucket(properties.bucket).key(path) }
            .runCatching { join() }

        if (result.isFailure) {
            return null
        }

        return path.substringAfterLast("/")
            .let(::Path)
    }

    override fun downloadAndUpload(request: DownloadAndUploadRequest) {
        val content = download(request.url)
            ?: return

        when(request) {
            is DownloadAndUploadRequest.ForPlaylistCover -> upload(request.toUploadRequest(content))
            is DownloadAndUploadRequest.ForPodcastCover ->  upload(request.toUploadRequest(content))
            is DownloadAndUploadRequest.ForItemCover ->     upload(request.toUploadRequest(content))
        }
    }

    override fun upload(request: UploadRequest) {
        val bucketRequest = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .key(request.path.toString())
            .build()

        val operation = when(request) {
            is UploadRequest.ForPlaylistCover  -> bucket.putObject(bucketRequest, AsyncRequestBody.fromBytes(request.content.byteArray))
            is UploadRequest.ForPodcastCover   -> bucket.putObject(bucketRequest, AsyncRequestBody.fromBytes(request.content.byteArray))
            is UploadRequest.ForItemCover      -> bucket.putObject(bucketRequest, AsyncRequestBody.fromBytes(request.content.byteArray))
            is UploadRequest.ForItemFromPath   -> bucket.putObject(bucketRequest, request.content)
            is UploadRequest.ForItemFromStream -> bucket.putObject(bucketRequest, AsyncRequestBody.fromInputStream { it
                .inputStream(request.content)
                .contentLength(request.length)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
            })
        }

        retry { operation.join() }
    }

    private fun download(url: URI): ByteArrayResource? = rcb.clone()
        .baseUrl(url.toASCIIString())
        .build()
        .get()
        .accept(MediaType.ALL, MediaType.APPLICATION_OCTET_STREAM)
        .retrieve()
        .body<ByteArrayResource>()

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
            .also { log.error("error during operation, operation canceled", it.exceptionOrNull()) }
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

    override fun toExternalUrl(r: ExternalUrlRequest): URI {
        val path = when(r) {
            is ExternalUrlRequest.ForPlaylist -> ".playlist/${r.playlistName}/${r.file.fileName}"
            is ExternalUrlRequest.ForItem -> "${r.podcastTitle}/${r.file.fileName}"
            is ExternalUrlRequest.ForPodcast -> "${r.podcastTitle}/${r.file.fileName}"
        }

        return preSignerBuilder(r.host).presignGetObject { sign -> sign
            .signatureDuration(Duration.ofDays(1))
            .getObjectRequest { request -> request
                .bucket(properties.bucket)
                .key(path)
            }
        }.url().toURI()
    }
}

private fun DownloadAndUploadRequest.ForItemCover.extension() = Path(coverUrl.path).extension.ifBlank { "jpg" }
private fun DownloadAndUploadRequest.ForPlaylistCover.extension() = Path(cover.url.path).extension.ifBlank { "jpg" }
private fun DownloadAndUploadRequest.ForPodcastCover.extension() = Path(coverUrl.path).extension.ifBlank { "jpg" }

data class MovePodcastRequest(val id: UUID, val from: String, val to: String)
data class FileMetaData(val contentType: String, val size: Long)
