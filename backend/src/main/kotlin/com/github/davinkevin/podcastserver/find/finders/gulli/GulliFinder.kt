package com.github.davinkevin.podcastserver.find.finders.gulli

import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.fetchFindCoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import org.jsoup.Jsoup
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

class GulliFinder(
    private val client: RestClient,
    private val image: ImageService
): Finder {

    override fun findPodcastInformation(url: String): FindPodcastInformation? {
        val path = url.substringAfterLast("replay.gulli.fr")

        val page = client
            .get()
            .uri(path)
            .retrieve()
            .body<String>()
            ?: return null

        val html = Jsoup.parse(page, url)
        val cover3 = html.select(".container_full .visuel img")
            .firstOrNull()
            ?.let { image.fetchFindCoverInformation(URI(it.attr("src"))) }

        return FindPodcastInformation(
            title = html.select("ol.breadcrumb li.active").first()!!.text(),
            description = html.select(".container_full .description").text(),
            url = URI(url),
            type = "Gulli",
            cover = cover3,
        )
    }

    override fun compatibility(url: String): Int = when {
        "replay.gulli.fr" in url -> 1
        else -> Integer.MAX_VALUE
    }
}
