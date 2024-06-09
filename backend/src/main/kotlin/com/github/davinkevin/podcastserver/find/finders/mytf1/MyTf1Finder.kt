package com.github.davinkevin.podcastserver.find.finders.mytf1

import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.fetchFindCoverInformation
import com.github.davinkevin.podcastserver.find.finders.meta
import com.github.davinkevin.podcastserver.service.image.ImageService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Created by kevin on 12/01/2020
 */
class MyTf1Finder(
        private val client: RestClient,
        private val image: ImageService
): Finder {

    override fun findPodcastInformation(url: String): FindPodcastInformation? {
        val path = url.substringAfter("www.tf1.fr")

        val content = client
            .get()
            .uri(path)
            .retrieve()
            .body<String>()
            ?: return null

        val html = Jsoup.parse(content, url)
        val cover = findCover(html)

        return FindPodcastInformation(
            title = html.meta("property=og:title"),
            description = html.meta("property=og:description"),
            url = url.let(::URI),
            cover = cover,
            type = "MyTF1"
        )
    }

    override fun findInformation(url: String): Mono<FindPodcastInformation> {
        return Mono.fromCallable { findPodcastInformation(url) }
    }

    private fun findCover(d: Document): FindCoverInformation? {
        val ogImage = d.meta("property=og:image")

        val url = when {
            ogImage.isEmpty() -> return null
            ogImage.startsWith("//") -> "https:$ogImage"
            else -> ogImage
        }

        return image.fetchFindCoverInformation(URI(url))
    }

    override fun compatibility(url: String): Int = when {
        "www.tf1.fr" in url -> 1
        else -> Integer.MAX_VALUE
    }

}
