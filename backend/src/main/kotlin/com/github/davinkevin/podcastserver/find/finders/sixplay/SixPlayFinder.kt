package com.github.davinkevin.podcastserver.find.finders.sixplay

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.fetchCoverInformationOrOption
import com.github.davinkevin.podcastserver.find.finders.Finder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.kotlin.core.util.function.component3
import java.net.URI
import java.util.*
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

class SixPlayFinder(
        private val client: WebClient,
        private val image: ImageService,
        private val mapper: ObjectMapper
): Finder {

    override fun findInformation(url: String): Mono<FindPodcastInformation> {
        val path = url.substringAfter("www.6play.fr")

        return client
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono<String>()
                .map { Jsoup.parse(it, url) }
                .flatMap { Mono.zip(findCover(it), findDescription(it), it.toMono()) }
                .map { (cover, desc, d) -> FindPodcastInformation(
                        title = d.select("div.description-program__title").text(),
                        url = d.select("link[rel=canonical]").attr("href").let(::URI),
                        description = desc,
                        cover = cover.orNull(),
                        type = "SixPlay"
                ) }
    }

    private fun findDescription(d: Document): Mono<String> {
        val scriptTag = d
                .select("script")
                .firstOrNull { it.html().contains("root.") }
                ?: return "".toMono()

        val jsonAsString = scriptTag
                .html()
                .replace("\n".toRegex(), "")
                .substringAfter("=")
                .substringBefore("}(this));")
                .replace("\\\"", "\"")
                .replace("\\\\\"", "\\\"")
                .trim(';', '"', ' ')

        val program = mapper
                .readValue<SixPlayRoot>(jsonAsString)
                .program
                .programsById
                ?.values
                ?.firstOrNull() ?: return "".toMono()

        return program.description.toMono()
    }

    private fun findCover(d: Document): Mono<Optional<FindCoverInformation>> {
        val imageTag = d
                .select("div.header-image__image")
                .asSequence()
                .map { it.attr("style") }
                .filterNot { it.contains("blur") }
                .flatMap { it.split(";").asSequence() }
                .firstOrNull { it.contains("background-image") }
                ?: return Optional.empty<FindCoverInformation>().toMono()

        val url = imageTag
                .substringAfter("(")
                .substringBefore(")")
                .let(::URI)

        return image.fetchCoverInformationOrOption(url)
    }

    override fun compatibility(url: String?): Int =
            if (url != null && url.contains("www.6play.fr")) 1
            else Int.MAX_VALUE
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SixPlayRoot(
        val program: Program
) {
    data class Program(val programsById: Map<String, ProgramDescription>?) {
        data class ProgramDescription(val description: String = "")
    }
}
