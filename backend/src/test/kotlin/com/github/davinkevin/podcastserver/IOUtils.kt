package com.github.davinkevin.podcastserver

import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import org.apache.commons.codec.digest.DigestUtils
import org.jdom2.input.SAXBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.BufferedReader
import java.io.IOException
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

fun toPath(uri: String): arrow.core.Try<Path> =
        arrow.core.Try { IOUtils::class.java.getResource(uri) }
                .map { it.toURI() }
                .map { Paths.get(it) }

fun fileAsXml(uri: String): org.jdom2.Document? = toPath(uri)
        .map { it.toFile() }
        .map { f -> SAXBuilder().build(f) }
        .toOption()
        .orNull()

fun fileAsHtml(uri: String, baseUri: String = ""): arrow.core.Option<Document> = toPath(uri)
        .map { it.toFile() }
        .map { Jsoup.parse(it, "UTF-8", baseUri) }
        .toOption()

fun fileAsString(uri: String): String {
    return toPath(uri)
            .map { Files.newInputStream(it) }
            .map { it.bufferedReader().use { it.readText() } }
            .getOrElse { throw RuntimeException("Error during file fetching", it) }
}

fun fileAsJson(path: String): arrow.core.Option<DocumentContext> = arrow.core.Option.just(path)
        .flatMap { arrow.core.Option.fromNullable(IOUtils::class.java.getResource(it)) }
        .map { it.toURI() }
        .map { Paths.get(it) }
        .map { it.toFile() }
        .map { PARSER.parse(it) }

fun fileAsReader(file: String): BufferedReader = toPath(file)
        .map { Files.newBufferedReader(it) }
        .getOrElse{ throw IOException("File $file not found") }

fun urlAsStream(url: String): InputStream = arrow.core.Try { URL(url).openStream() }
        .getOrElse{ throw RuntimeException(it) }

fun stringAsJson(text: String): DocumentContext = PARSER.parse(text)

fun digest(text: String): String = DigestUtils.md5Hex(text)
