package com.github.davinkevin.podcastserver.find.finders.mytf1

import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.fetchCoverInformationOrOption
import com.github.davinkevin.podcastserver.service.image.ImageService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.util.*

/**
 * Created by kevin on 12/01/2020
 */
class MyTf1Finder(
        private val client: WebClient,
        private val image: ImageService
): Finder {

    override fun findInformation(url: String): Mono<FindPodcastInformation> {
        val path = url.substringAfter("www.tf1.fr")

        return client
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono<String>()
                .map { Jsoup.parse(it, url) }
                .flatMap { Mono.zip(findCover(it), it.toMono()) }
                .map { (cover, d) ->  FindPodcastInformation(
                        title = d.select("meta[property=og:title]").attr("content"),
                        description = d.select("meta[property=og:description]").attr("content"),
                        url = url.let(::URI),
                        cover = cover.orNull(),
                        type = "MyTF1"
                ) }
    }

    private fun findCover(d: Document): Mono<Optional<FindCoverInformation>> {
        val ogImage = d.select("meta[property=og:image]")
                .attr("content")

        if (ogImage.isEmpty()) {
            return Optional.empty<FindCoverInformation>().toMono()
        }

        val url = when {
            ogImage.startsWith("//") -> "https:$ogImage"
            else -> ogImage
        }
                .let(::URI)

        return image.fetchCoverInformationOrOption(url)
    }

    override fun compatibility(url: String): Int = when {
        "www.tf1.fr" in url -> 1
        else -> Integer.MAX_VALUE
    }

}
