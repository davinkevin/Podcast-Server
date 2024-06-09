package com.github.davinkevin.podcastserver.find.finders.itunes

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.rss.RSSFinder
import com.github.davinkevin.podcastserver.utils.MatcherExtractor.Companion.from
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

class ItunesFinder(
    private val rssFinder: RSSFinder,
    private val wc: WebClient,
    private val om: ObjectMapper
) : Finder {

    override fun findInformation(url: String): Mono<FindPodcastInformation> {
        val id = ARTIST_ID.on(url).group(1) ?: return Mono.empty()

        return wc.get()
            .uri { it.path("lookup").queryParam("id", id).build() }
            .retrieve()
            .bodyToMono<String>()
            .map { om.readTree(it)["results"][0]["feedUrl"].asText() }
            .flatMap { rssFinder.findInformation(it) }
    }

    override fun compatibility(url: String): Int = when {
        "itunes.apple.com" in url -> 1
        "podcasts.apple.com" in url -> 1
        else -> Integer.MAX_VALUE
    }

    companion object {
        private val ARTIST_ID = from(".*id=?([\\d]+).*")
    }
}
