package com.github.davinkevin.podcastserver.service

import arrow.core.Try
import arrow.core.getOrElse
import arrow.core.orElse
import arrow.core.toOption
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.manager.worker.youtube.YoutubeUpdater.YOUTUBE
import org.apache.commons.io.FilenameUtils
import org.apache.tika.Tika
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by kevin on 22/07/2018
 */
@Service
class MimeTypeService(val tikaProbeContentType: TikaProbeContentType) {
    companion object {
        val mimeMap = mapOf(
                "mp4" to "video/mp4",
                "mp3" to "audio/mp3",
                "flv" to "video/flv",
                "webm" to "video/webm",
                "" to "video/mp4"
        )
    }

    fun getMimeType(extension: String): String {
        if (extension.isEmpty()) {
            return "application/octet-stream"
        }

        return mimeMap[extension]
                .toOption()
                .getOrElse { "unknown/$extension" }
    }

    fun getExtension(item: Item): String {
        if (item.mimeType != null) {
            return item.mimeType
                    .replace("audio/", ".")
                    .replace("video/", ".")
        }

        if (item.podcast.type == YOUTUBE || !item.url.contains(".") ) {
            return ".mp4"
        }

        return "." + FilenameUtils.getExtension(item.url)
    }

    // https://odoepner.wordpress.com/2013/07/29/transparently-improve-java-7-mime-type-recognition-with-apache-tika/
    fun probeContentType(file: Path) =
            filesProbeContentType(file)
                    .orElse { tikaProbeContentType.probeContentType(file) }
                    .getOrElse({ getMimeType(FilenameUtils.getExtension(file.fileName.toString())) })

    private fun filesProbeContentType(file: Path) =
            Try { Files.probeContentType(file) }
                    .filter { it != null }
                    .toOption()
}

class TikaProbeContentType(val tika: Tika) {
    fun probeContentType(file: Path) =
            Try { tika.detect(file) }
                    .filter { it != null }
                    .toOption()
}