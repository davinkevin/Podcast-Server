package com.github.davinkevin.podcastserver.find.finders.dailymotion

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.find.FindCoverInformation
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.orNull
import com.github.davinkevin.podcastserver.find.toMonoOption
import com.github.davinkevin.podcastserver.manager.worker.Finder
import com.github.davinkevin.podcastserver.service.image.CoverInformation
import com.github.davinkevin.podcastserver.utils.MatcherExtractor
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URI
import com.github.davinkevin.podcastserver.service.image.ImageServiceV2 as ImageService

/**
 * Created by kevin on 01/11/2019
 */
class DailymotionFinder(
        private val wc: WebClient,
        private val image: ImageService
): Finder {

    override fun find(url: String): Podcast = TODO("not required anymore")

    override fun findInformation(url: String): Mono<FindPodcastInformation> {
        val userName = USER_NAME_EXTRACTOR.on(url).groupk(1) ?: return RuntimeException("username not found int url $url").toMono()

        return wc.get()
                .uri { it
                        .pathSegment("user")
                        .path(userName)
                        .queryParam("fields", "avatar_720_url,description,username")
                        .build()
                }
                .retrieve()
                .bodyToMono<DailymotionUserDetail>()
                .flatMap { p -> image
                        .fetchCoverInformation(URI(p.avatar))
                        .map { it.toFindCover() }
                        .toMonoOption()
                        .zipWith(p.toMono())
                }
                .map { (cover, podcast) -> FindPodcastInformation(
                        title = podcast.username,
                        url = URI(url),
                        description = podcast.description ?: "",
                        type = "Dailymotion",
                        cover = cover.orNull()
                ) }
    }

    override fun compatibility(url: String?): Int {
        return if ((url ?: "").contains("www.dailymotion.com")) 1
        else Int.MAX_VALUE
    }


}

private val USER_NAME_EXTRACTOR = MatcherExtractor.from("^.+dailymotion.com/(.*)")

@JsonIgnoreProperties(ignoreUnknown = true)
private class DailymotionUserDetail(
        @JsonProperty("avatar_720_url") val avatar: String,
        val username: String,
        val description: String?
)

private fun CoverInformation.toFindCover() = FindCoverInformation(height, width, url)
