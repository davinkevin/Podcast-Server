package com.github.davinkevin.podcastserver

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by kevin on 23/07/2016.
 */
private object IOUtils {}

fun toPath(uri: String): Path {
    val file = IOUtils::class.java.getResource(uri)?.toURI() ?: error("file $uri not found")
    return Paths.get(file)
}

fun fileAsString(uri: String): String {
    return Files.newInputStream(toPath(uri))
            .bufferedReader()
            .use { it.readText() }
}

fun fileAsByteArray(uri: String): ByteArray {
    return Files.readAllBytes(toPath(uri))
}
