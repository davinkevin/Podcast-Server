package com.github.davinkevin.podcastserver.manager.worker.youtube

import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.manager.worker.Finder
import com.github.davinkevin.podcastserver.service.CoverInformation
import com.github.davinkevin.podcastserver.service.ImageService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.io.ByteArrayInputStream
import java.net.URI

/**
 * Created by kevin on 22/02/15
 */
@Service
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
            .map { ByteArrayInputStream(it.toByteArray(Charsets.UTF_8)) }
            .map { Jsoup.parse(it, "UTF-8", url) }
            .map { toFindPodcastInformation(it, url) }

    override fun find(url: String): Podcast = TODO("not required anymore")

    private fun toFindPodcastInformation(p: Document, anUrl: String) = FindPodcastInformation (
        url = URI(anUrl),
        type = "Youtube",
        title = p.meta("title"),
        description = p.meta("description"),
        cover = findCover(p)?.toFindCover()
    )

    private fun findCover(page: Document): CoverInformation? {
        val coverUrl = page
                .select("img.channel-header-profile-image")
                .attr("src")

        return  if(coverUrl.isEmpty()) null
                else imageService.fetchCoverInformation(coverUrl)
    }

    override fun compatibility(url: String?) = _compatibility(url)
}

private fun Document.meta(s: String) = this.select("meta[name=$s]").attr("content")
private fun CoverInformation.toFindCover() = FindCoverInformation(height, width, url)
