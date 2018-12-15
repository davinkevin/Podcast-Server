package com.github.davinkevin.podcastserver

import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import io.vavr.control.Option
import io.vavr.jackson.datatype.VavrModule
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
object IOUtils {

    private val PARSER = JsonPath.using(Configuration.builder().mappingProvider(JacksonMappingProvider(
            ObjectMapper()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .registerModules(
                            Hibernate5Module()
                                    .enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING)
                                    .disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION),
                            JavaTimeModule(),
                            VavrModule()
                    )
    )).build())

    const val TEMPORARY_EXTENSION = ".psdownload"
    @JvmField val ROOT_TEST_PATH = Paths.get("/tmp/podcast-server-test/")!!


    @JvmStatic fun toPath(uri: String): arrow.core.Try<Path> =
            arrow.core.Try { IOUtils::class.java.getResource(uri) }
                    .map { it.toURI() }
                    .map { Paths.get(it) }

    @JvmStatic fun fileAsXml(uri: String): Option<org.jdom2.Document> {
        return toPath(uri)
                .map { it.toFile() }
                .map { f -> SAXBuilder().build(f) }
                .toOption()
                .toVΛVΓ()
    }

    @JvmStatic
    @JvmOverloads
    fun fileAsHtml(uri: String, baseUri: String = ""): Option<Document> {
        return toPath(uri)
                .map { it.toFile() }
                .map { Jsoup.parse(it, "UTF-8", baseUri) }
                .toOption()
                .toVΛVΓ()
    }

    @JvmStatic fun fileAsString(uri: String): String {
        return toPath(uri)
                .map { Files.newInputStream(it) }
                .map { it.bufferedReader().use { it.readText() } }
                .getOrElse { throw RuntimeException("Error during file fetching", it) }
    }

    @JvmStatic fun fileAsJson(path: String): Option<DocumentContext> {
        return arrow.core.Option.just(path)
                .flatMap { arrow.core.Option.fromNullable(IOUtils::class.java.getResource(it)) }
                .map { it.toURI() }
                .map { Paths.get(it) }
                .map { it.toFile() }
                .map { PARSER.parse(it) }
                .toVΛVΓ()
    }

    @JvmStatic fun fileAsStream(file: String): InputStream {
        return IOUtils::class.java.getResourceAsStream(file)
    }

    @JvmStatic fun fileAsReader(file: String): BufferedReader {
        return toPath(file)
                .map { Files.newBufferedReader(it) }
                .getOrElse{ throw IOException("File $file not found") }
    }

    @JvmStatic fun urlAsStream(url: String): InputStream {
        return arrow.core.Try { URL(url).openStream() }
                .getOrElse{ throw RuntimeException(it) }
    }

    @JvmStatic fun stringAsJson(text: String): DocumentContext {
        return PARSER.parse(text)
    }

    @JvmStatic fun stringAsHtml(html: String): Document {
        return arrow.core.Try { Jsoup.parse(html) }.getOrElse { throw RuntimeException("Error during conversion from string to html", it) }
    }

    @JvmStatic fun get(uri: String): Path {
        return Paths.get(IOUtils::class.java.getResource(uri).toURI())
    }

    @JvmStatic fun digest(text: String): String {
        return DigestUtils.md5Hex(text)
    }
}
