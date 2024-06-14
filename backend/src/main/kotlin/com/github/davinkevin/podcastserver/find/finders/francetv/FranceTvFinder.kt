package com.github.davinkevin.podcastserver.find.finders.francetv

import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.fetchFindCoverInformation
import com.github.davinkevin.podcastserver.find.finders.meta
import com.github.davinkevin.podcastserver.service.image.ImageService
import org.jsoup.Jsoup
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import reactor.kotlin.core.publisher.toMono
import java.net.URI

/**
 * Created by kevin on 01/11/2019
 */
class FranceTvFinder(
        private val client: RestClient,
        private val image: ImageService
): Finder {

    override fun findPodcastInformation(url: String): FindPodcastInformation? {
        val path = url.substringAfterLast("www.france.tv")

        val page = client
            .get()
            .uri(path)
            .retrieve()
            .body<String>()
            ?: return null

        val html = Jsoup.parse(page, url)
        val coverUrl = html.meta("property=og:image").let(::URI)
        val cover = image.fetchFindCoverInformation(coverUrl)

        return FindPodcastInformation(
            title = html.meta("property=og:title"),
            description = html.meta("property=og:description"),
            type = "FranceTv",
            url = html.meta("property=og:url").let(::URI),
            cover = cover
        )
    }

    override fun findInformation(url: String) = findPodcastInformation(url).toMono()
    override fun compatibility(url: String): Int = when {
        "www.france.tv" in url -> 1
        else -> Int.MAX_VALUE
    }
}
