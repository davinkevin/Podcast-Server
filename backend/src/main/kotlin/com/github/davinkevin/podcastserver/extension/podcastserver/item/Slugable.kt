package com.github.davinkevin.podcastserver.extension.podcastserver.item

import java.nio.file.Path
import java.text.Normalizer
import java.util.*
import kotlin.io.path.extension

interface Sluggable {

    val title: String
    val mimeType: String
    val fileName: Path?

    fun slug(): String {
        val extension = fileName?.extension ?: mimeType.substringAfter("/")
        val sluggedTitle = Normalizer.normalize(title, Normalizer.Form.NFD)
            .lowercase(Locale.getDefault())
            .replace("\\p{IsM}+".toRegex(), "")
            .replace("\\p{IsP}+".toRegex(), " ")
            .trim()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-zA-Z0-9.-]".toRegex(), "_")

        return "$sluggedTitle.$extension"
    }
}