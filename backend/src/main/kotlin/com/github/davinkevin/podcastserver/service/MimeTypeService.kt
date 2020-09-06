package com.github.davinkevin.podcastserver.service

import org.apache.commons.io.FilenameUtils
import org.apache.tika.Tika
import java.nio.file.Files
import java.nio.file.Path
import kotlin.*

/**
 * Created by kevin on 22/07/2018
 */
class MimeTypeService(
        private val tika: Tika,
        private val mimeMap: Map<String, String>
) {

    private fun fromExtension(extension: String): String {
        if (extension.isEmpty()) {
            return "application/octet-stream"
        }

        return mimeMap[extension] ?: "unknown/$extension"
    }

    // https://odoepner.wordpress.com/2013/07/29/transparently-improve-java-7-mime-type-recognition-with-apache-tika/
    fun probeContentType(file: Path): String {
        return runCatching { Files.probeContentType(file) }.getOrNull()
                ?: runCatching { tika.detect(file) }.getOrNull()
                ?: fromExtension(FilenameUtils.getExtension(file.fileName.toString()))
    }

}


