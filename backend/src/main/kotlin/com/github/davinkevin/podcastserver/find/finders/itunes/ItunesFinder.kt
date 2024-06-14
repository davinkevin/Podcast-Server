package com.github.davinkevin.podcastserver.find.finders.itunes

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.rss.RSSFinder
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

class ItunesFinder(
    private val rssFinder: RSSFinder,
    private val wc: RestClient,
    private val om: ObjectMapper
) : Finder {

    override fun findPodcastInformation(url: String): FindPodcastInformation? {
        val matches = ARTIST_ID.find(url) ?: return null
        val id = matches.groups["id"]!!.value

        val response = wc.get()
            .uri { it.path("lookup").queryParam("id", id).build() }
            .retrieve()
            .body<String>()
            ?: return null

        val feedUrl = om.readValue<ItunesResponse>(response)
            .results.first().feedUrl

        return rssFinder.findPodcastInformation(feedUrl)
    }

    override fun compatibility(url: String): Int = when {
        "itunes.apple.com" in url -> 1
        "podcasts.apple.com" in url -> 1
        else -> Integer.MAX_VALUE
    }
}

private val ARTIST_ID = ".*id=?(?<id>\\d+).*".toRegex()

@JsonIgnoreProperties(ignoreUnknown = true)
private class ItunesResponse(val results: List<Result>) {
    class Result(val feedUrl: String)
}