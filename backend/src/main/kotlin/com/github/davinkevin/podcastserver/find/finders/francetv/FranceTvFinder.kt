package com.github.davinkevin.podcastserver.find.finders.francetv

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
import java.net.URI
import java.util.*
import com.github.davinkevin.podcastserver.service.image.ImageService

/**
 * Created by kevin on 01/11/2019
 */
class FranceTvFinder(
        private val client: WebClient,
        private val image: ImageService
): Finder {

    override fun findInformation(url: String): Mono<FindPodcastInformation> {
        val path = url.substringAfterLast("www.france.tv")

        return client
                .get()
                .uri(path)
                .retrieve()
                .bodyToMono<String>()
                .map { Jsoup.parse(it, url) }
                .flatMap { d -> findCover(d).zipWith(d.toMono()) }
                .map { (cover, d) -> FindPodcastInformation(
                        title = d.select("meta[property=og:title]").attr("content"),
                        description = d.select("meta[property=og:description]").attr("content"),
                        type = "FranceTv",
                        url = URI(d.select("meta[property=og:url]").attr("content")),
                        cover = cover.orNull()
                ) }
    }

    private fun findCover(d: Document): Mono<Optional<FindCoverInformation>> {
        val coverUrl = URI(d.select("meta[property=og:image]").attr("content"))

        return image.fetchCoverInformationOrOption(coverUrl)
    }

    override fun compatibility(url: String?): Int {
        return if((url ?: "").contains("www.france.tv")) 1
        else Int.MAX_VALUE
    }
}
