package com.github.davinkevin.podcastserver.find.finders.mycanal

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.fetchCoverInformationOrOption
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.util.*

/**
 * Created by kevin on 03/11/2019
 */
class MyCanalFinder(
        private val client: WebClient,
        private val image: ImageServiceV2,
        private val mapper: ObjectMapper
): Finder {

    override fun findInformation(url: String): Mono<FindPodcastInformation> {
        val path = url.substringAfter("www.canalplus.com")

        return client
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono<String>()
                .map { Jsoup.parse(it, url) }
                .flatMap { extractJsonConfig(it) }
                .flatMap { toPodcastInformation(it) }
    }

    private fun extractJsonConfig(d: Document): Mono<String> {
        val scriptTag = d.body()
                .select("script")
                .map { it.html() }
                .firstOrNull { e -> e.contains("app_config") }
                ?: return RuntimeException("app_config not found").toMono()

        if (!scriptTag.contains("__data=") || !scriptTag.contains("};")) {
            return RuntimeException("app_config has change its structure").toMono()
        }

        return scriptTag
                .substringAfter("__data=")
                .substringBefore("};")
                .let { "$it}" }
                .toMono()
    }

    private fun toPodcastInformation(json: String): Mono<FindPodcastInformation> {

        val item = mapper.readValue<MyCanalRoot>(json)

        val cover = if(item.templates.landing.cover?.image != null) {
            image.fetchCoverInformationOrOption(URI(item.templates.landing.cover.image))
        } else {
            Optional.empty<FindCoverInformation>().toMono()
        }

        return cover.map { FindPodcastInformation(
                title = item.page.displayName,
                url = URI("https://www.canalplus.com${item.page.pathname}"),
                type = "MyCanal",
                cover = it.orNull()
        ) }
    }

    override fun compatibility(url: String?): Int {
        if (url.isNullOrEmpty()) return Int.MAX_VALUE
        if ("www.canalplus.com" in url) return 1
        return Int.MAX_VALUE
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class MyCanalRoot(val templates: Templates, val page: Page) {

    data class Templates(val landing: Landing)
    data class Landing(val cover: Cover?)
    data class Cover(val image: String?)

    data class Page(val displayName: String, val pathname: String)
}
