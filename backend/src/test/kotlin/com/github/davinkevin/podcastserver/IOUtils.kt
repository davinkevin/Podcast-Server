package com.github.davinkevin.podcastserver

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import org.jdom2.input.SAXBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.BufferedReader
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by kevin on 23/07/2016.
 */
private object IOUtils {}

private val PARSER = JsonPath.using(Configuration.builder().mappingProvider(JacksonMappingProvider(
        ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModules(
                        JavaTimeModule(),
                        KotlinModule()
                )
)).build())

const val TEMPORARY_EXTENSION = ".psdownload"
val ROOT_TEST_PATH: Path = Paths.get("/tmp/podcast-server-test/")

fun toPath(uri: String): Path {
    val file = IOUtils::class.java.getResource(uri).toURI() ?: error("file $uri not found")
    return Paths.get(file)
}

fun fileAsXml(uri: String): org.jdom2.Document? {
    return SAXBuilder().build(toPath(uri).toFile())
}

fun fileAsHtml(uri: String, baseUri: String = ""): Document {
    val file = toPath(uri).toFile()
    return Jsoup.parse(file, "UTF-8", baseUri)
}

fun fileAsString(uri: String): String {
    return Files.newInputStream(toPath(uri))
            .bufferedReader()
            .use { it.readText() }
}

fun fileAsReader(file: String): BufferedReader {
    return Files.newBufferedReader(toPath(file)) ?: error("File $file not found")
}

fun urlAsStream(url: String): InputStream {
    return URL(url).openStream()
}
