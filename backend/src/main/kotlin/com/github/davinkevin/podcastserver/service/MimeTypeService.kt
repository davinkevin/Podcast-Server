package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.extension.java.util.tryOrNull
import org.apache.commons.io.FilenameUtils
import org.apache.tika.Tika
import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by kevin on 22/07/2018
 */
class MimeTypeService(private val tika: Tika) {

    companion object {
        val mimeMap = mapOf(
                "mp4" to "video/mp4",
                "mp3" to "audio/mp3",
                "flv" to "video/flv",
                "webm" to "video/webm",
                "" to "video/mp4"
        )
    }

    private fun fromExtension(extension: String): String {
        if (extension.isEmpty()) {
            return "application/octet-stream"
        }

        return mimeMap[extension] ?: "unknown/$extension"
    }

    // https://odoepner.wordpress.com/2013/07/29/transparently-improve-java-7-mime-type-recognition-with-apache-tika/
    fun probeContentType(file: Path): String =
            tryOrNull { Files.probeContentType(file) }
            ?: tryOrNull { tika.detect(file) }
            ?: fromExtension(FilenameUtils.getExtension(file.fileName.toString()))

}


