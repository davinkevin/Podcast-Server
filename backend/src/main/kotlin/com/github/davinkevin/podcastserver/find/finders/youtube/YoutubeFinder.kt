package com.github.davinkevin.podcastserver.find.finders.youtube

import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.extension.java.util.orNull
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.fetchCoverInformationOrOption
import com.github.davinkevin.podcastserver.manager.worker.Finder
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.update.updaters.youtube._compatibility
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import java.util.*
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

/**
 * Created by kevin on 22/02/15
 */
class YoutubeFinder(
        private val imageService: ImageService,
        private val wcb: WebClient.Builder
) : Finder {

    override fun findInformation(url: String) = wcb
            .baseUrl(url)
            .build()
            .get()
            .retrieve()
            .bodyToMono<String>()
            .map { Jsoup.parse(it, url) }
            .flatMap { doc -> findCover(doc).zipWith(doc.toMono()) }
            .map { (cover, doc) -> FindPodcastInformation (
                    url = URI(url),
                    type = "Youtube",
                    title = doc.meta("title"),
                    description = doc.meta("description"),
                    cover = cover.orNull()
            ) }

    override fun find(url: String): Podcast = TODO("not required anymore")

    private fun findCover(page: Document): Mono<Optional<FindCoverInformation>> {
        return page
                .select("img.channel-header-profile-image")
                .attr("src")
                .replace("s100", "s1400")
                .toMono()
                .filter { it.isNotEmpty() }
                .flatMap { imageService.fetchCoverInformationOrOption(URI(it)) }
                .switchIfEmpty { Optional.empty<FindCoverInformation>().toMono() }
    }

    override fun compatibility(url: String?) = _compatibility(url)
}

private fun Document.meta(s: String) = this.select("meta[name=$s]").attr("content")
private fun CoverInformation.toFindCover() = FindCoverInformation(height, width, url)
