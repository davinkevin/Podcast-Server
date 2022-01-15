package com.github.davinkevin.podcastserver.service.storage

import com.github.davinkevin.podcastserver.cover.DeleteCoverRequest
import com.github.davinkevin.podcastserver.item.DeleteItemRequest
import com.github.davinkevin.podcastserver.item.Item
import com.github.davinkevin.podcastserver.podcast.CoverForPodcast
import com.github.davinkevin.podcastserver.podcast.DeletePodcastRequest
import com.github.davinkevin.podcastserver.podcast.Podcast
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.util.retry.Retry
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.*

/**
 * Created by kevin on 2019-02-09
 */
class FileStorageService(
    private val wcb: WebClient.Builder,
    private val bucket: S3AsyncClient,
    private val preSignerBuilder: (URI) -> S3Presigner,
    private val properties: StorageProperties,
) {

    private val log = LoggerFactory.getLogger(FileStorageService::class.java)

    fun deletePodcast(podcast: DeletePodcastRequest) = Mono.defer {
        log.info("Deletion of podcast {}", podcast.title)

        bucket.listObjects { it.bucket(properties.bucket).prefix(podcast.title) }.toMono()
            .flatMapIterable { it.contents() }
            .flatMap { bucket.deleteObject(it.toDeleteRequest()).toMono() }
            .then(true.toMono())
            .onErrorReturn(false)
    }

    fun deleteItem(item: DeleteItemRequest): Mono<Boolean> = Mono.defer {
        val path = "${item.podcastTitle}/${item.fileName}"

        log.info("Deletion of file {}", path)

        bucket.deleteObject { it.bucket(properties.bucket).key(path) }.toMono()
            .map { true }
            .onErrorReturn(false)
    }

    fun deleteCover(cover: DeleteCoverRequest): Mono<Boolean> = Mono.defer {
        val path = "${cover.podcast.title}/${cover.item.id}.${cover.extension}"

        log.info("Deletion of file {}", path)

        bucket.deleteObject { it.bucket(properties.bucket).key(path) }.toMono()
            .map { true }
            .onErrorReturn(false)
    }

    fun coverExists(p: Podcast): Mono<String> = coverExists(p.title, p.id, p.cover.extension())
    fun coverExists(i: Item): Mono<String> = coverExists(i.podcast.title, i.id, i.cover.extension())
    fun coverExists(podcastTitle: String, itemId: UUID, extension: String): Mono<String> {
        val path = "$podcastTitle/$itemId.$extension"
        return bucket.headObject { it.bucket(properties.bucket).key(path) }.toMono()
            .map { true }
            .onErrorReturn(false)
            .filter { it }
            .map { path.substringAfterLast("/") }
    }

    private fun download(url: URI) = wcb.clone()
        .baseUrl(url.toASCIIString())
        .build()
        .get()
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .retrieve()
        .bodyToMono(ByteArrayResource::class.java)

    private fun upload(key: String, resource: ByteArrayResource): Mono<PutObjectResponse> {
        val request = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .key(key)
            .build()

        return bucket.putObject(request, AsyncRequestBody.fromBytes(resource.byteArray)).toMono()
    }

    fun downloadPodcastCover(podcast: Podcast): Mono<Void> =
        download(podcast.cover.url)
            .flatMap { upload("""${podcast.title}/${podcast.id}.${podcast.cover.extension()}""", it) }
            .then()

    fun downloadItemCover(item: Item): Mono<Void> =
        download(item.cover.url)
            .flatMap { upload("""${item.podcast.title}/${item.id}.${item.cover.extension()}""", it) }
            .then()

    fun movePodcast(request: MovePodcastRequest): Mono<Void> = Mono.defer {
        val listRequest = ListObjectsRequest.builder()
            .bucket(properties.bucket)
            .prefix(request.from)
            .build()

        bucket.listObjects(listRequest).toMono()
            .flatMapIterable { it.contents() }
            .flatMap { bucket
                .copyObject(it.toCopyRequest(request.to)).toMono()
                .then(Mono.defer { bucket.deleteObject(it.toDeleteRequest()).toMono() })
            }
            .then()
    }

    fun cache(filePart: FilePart, destination: Path): Mono<Path> = Mono.defer {
        val tempLocation = Files.createTempDirectory("upload-temp")
            .resolve(destination.fileName)

        filePart.transferTo(tempLocation)
            .then(tempLocation.toMono())
    }

    fun upload(podcastTitle: String, file: Path): Mono<PutObjectResponse> {
        val request = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .key("$podcastTitle/${file.fileName}")
            .build()

        return bucket.putObject(request, file).toMono()
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            .delayUntil { Files.deleteIfExists(file).toMono() }
    }

    fun metadata(title: String, file: Path): Mono<FileMetaData> {
        val key = "$title/${file.fileName}"
        return Mono.defer { bucket.headObject() { it.bucket(properties.bucket).key(key) }.toMono() }
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
            .map { FileMetaData(contentType = it.contentType(), size = it.contentLength()) }
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

    fun initBucket(): Mono<Void> {
        return bucket.headBucket { it.bucket(properties.bucket) }.toMono()
            .doOnSuccess { log.info("🗂 Bucket already present") }
            .then()
            .onErrorResume { bucket.createBucket { it.bucket(properties.bucket) }.toMono()
                .doOnSuccess { log.info("✅ Bucket creation done") }
                .then()
            }
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

private fun CoverForPodcast.extension(): String = FilenameUtils.getExtension(url.toASCIIString()).ifBlank { "jpg" }
private fun Item.Cover.extension() = FilenameUtils.getExtension(url.toASCIIString()).ifBlank { "jpg" }

data class MovePodcastRequest(val id: UUID, val from: String, val to: String)
data class FileMetaData(val contentType: String, val size: Long)
data class FileDescriptor(val podcastTitle: String, val fileName: String)
