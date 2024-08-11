package com.github.davinkevin.podcastserver.service.storage

import org.springframework.core.io.ByteArrayResource
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.extension

interface Upload {
    fun downloadAndUpload(request: DownloadAndUploadRequest)
    fun upload(request: UploadRequest)
}

sealed interface UploadRequest {
    val path: Path

    data class ForPlaylistCover (override val path: Path,  val content: ByteArrayResource): UploadRequest
    data class ForPodcastCover  (override val path: Path,  val content: ByteArrayResource): UploadRequest
    data class ForItemCover     (override val path: Path,  val content: ByteArrayResource): UploadRequest
    data class ForItemFromPath  (val podcastTitle: String, val content: Path): UploadRequest {
        override val path: Path = Path("$podcastTitle/${content.fileName}")
    }
    data class ForItemFromStream(val podcastTitle: String, val fileName: Path, val content: InputStream): UploadRequest {
        override val path: Path = Path("$podcastTitle/${fileName.fileName}")
    }
}


sealed class DownloadAndUploadRequest(val url: URI) {
    data class ForPlaylistCover(val name: String, val cover: Cover): DownloadAndUploadRequest(cover.url) {
        data class Cover(val id: UUID, val url: URI)
    }
    data class ForPodcastCover(val id: UUID, val title: String, val coverUrl: URI): DownloadAndUploadRequest(coverUrl)
    data class ForItemCover(val id: UUID, val coverUrl: URI, val podcastTitle: String): DownloadAndUploadRequest(coverUrl)
}

internal fun DownloadAndUploadRequest.ForPlaylistCover.toUploadRequest(content: ByteArrayResource) = UploadRequest.ForPlaylistCover(
    path = Path(""".playlist/${name}/${cover.id}.${cover.url.extension()}"""),
    content = content,
)

internal fun DownloadAndUploadRequest.ForPodcastCover.toUploadRequest(content: ByteArrayResource) = UploadRequest.ForPodcastCover(
    path = Path("""${title}/${id}.${coverUrl.extension()}"""),
    content = content,
)

internal fun DownloadAndUploadRequest.ForItemCover.toUploadRequest(content: ByteArrayResource) = UploadRequest.ForItemCover(
    path = Path("""${podcastTitle}/${id}.${coverUrl.extension()}"""),
    content = content,
)

private fun URI.extension() = Path(path).extension.ifBlank { "jpg" }