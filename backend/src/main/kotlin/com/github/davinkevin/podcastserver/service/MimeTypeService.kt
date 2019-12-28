package com.github.davinkevin.podcastserver.service

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
            fileProbeContentType(file)
            ?: tikaProbeContentType(file)
            ?: fromExtension(FilenameUtils.getExtension(file.fileName.toString()))

    private fun fileProbeContentType(file: Path): String? = try { Files.probeContentType(file) } catch (e: Exception) { null }
    private fun tikaProbeContentType(file: Path): String? = try { tika.detect(file) } catch (e: Exception) { null }
}
