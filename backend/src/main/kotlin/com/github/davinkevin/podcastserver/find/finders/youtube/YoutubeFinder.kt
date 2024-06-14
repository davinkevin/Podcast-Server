package com.github.davinkevin.podcastserver.find.finders.youtube

import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.fetchFindCoverInformation
import com.github.davinkevin.podcastserver.find.finders.meta
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.youtube.youtubeCompatibility
import org.jsoup.Jsoup
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

/**
 * Created by kevin on 22/02/15
 */
class YoutubeFinder(
    private val imageService: ImageService,
    private val rcb: RestClient.Builder
) : Finder {

    override fun findPodcastInformation(url: String): FindPodcastInformation? {
        val content = rcb
            .clone()
            .baseUrl(url)
            .build()
            .get()
            .retrieve()
            .body<String>()
            ?: return null

        val html = Jsoup.parse(content, url)
        val cover = html.meta("property=og:image")
            .takeIf { it.isNotEmpty() }
            ?.let { imageService.fetchFindCoverInformation(URI(it)) }

        return FindPodcastInformation(
            url = URI(url),
            type = "Youtube",
            title = html.meta("property=og:title"),
            description = html.meta("name=description"),
            cover = cover
        )
    }

    override fun compatibility(url: String): Int = youtubeCompatibility(url)
}
