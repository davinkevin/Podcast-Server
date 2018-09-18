package com.github.davinkevin.podcastserver.service;

import arrow.core.Option
import arrow.core.Try
import arrow.core.getOrElse
import arrow.data.k
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class M3U8Service(val urlService: UrlService) {

    val log = LoggerFactory.getLogger(this.javaClass.name)!!

    fun findBestQuality(inputStream: InputStream): io.vavr.control.Option<String> =
            Try { inputStream.bufferedReader().useLines { it.toList() } }
                    .toOption()
                    .flatMap { findBestQuality(it) }
                    .toVΛVΓ()

    private fun findBestQuality(lines: List<String>): Option<String> =
            lines.k()
                    .zipWithNext()
                    .filter { it.first.startsWith("#EXT-X-STREAM-INF:") }
                    .map { it.first.replace("#EXT-X-STREAM-INF:", "") to it.second }
                    .map { constructParameters(it) }
                    .sortedByDescending { it.bandwidth }
                    .map { it.url }
                    .firstOption()

    private fun constructParameters(params: Pair<String, String>): M3U8Parameters {

        val bandwidth = params.first
                .replace(",avc1", "-avc1")
                .split(",")
                .toSet()
                .filter { it.length > 1 }
                .map { it.split("=") }
                .map { it[0] to it[1] }
                .first { it.first == "BANDWIDTH" }
                .second.toInt()

        return M3U8Parameters(params.second, bandwidth)
    }


    fun getM3U8UrlFormMultiStreamFile(url: String): String =
            Try { urlService.asReader(url).useLines {
                it
                        .filter { !it.startsWith("#") }
                        .last()
                        .substringBeforeLast("?")
                        .addDomainIfRelative(url)
            } }
                    .getOrElse {
                        log.error("Error during finding m3u8 from stream file for url {}", url, it)
                        throw RuntimeException(it)
                    }
}

private data class M3U8Parameters(val url: String, val bandwidth: Int)