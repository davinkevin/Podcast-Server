package com.github.davinkevin.podcastserver.find.finders.dailymotion

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.fetchFindCoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI

/**
 * Created by kevin on 01/11/2019
 */
class DailymotionFinder(
    private val wc: RestClient,
    private val image: ImageService
): Finder {

    override fun findPodcastInformation(url: String): FindPodcastInformation? {
        val matches = USER_NAME_EXTRACTOR.find(url) ?: throw RuntimeException("username not found in url $url")
        val userName = matches.groups["id"]!!.value

        val detail = wc.get()
            .uri { it
                .pathSegment("user")
                .path(userName)
                .queryParam("fields", "avatar_720_url,description,username")
                .build()
            }
            .retrieve()
            .body<DailymotionUserDetail>()
            ?: return null

        val cover = detail.avatar
            .let(::URI)
            .let(image::fetchFindCoverInformation)

        return FindPodcastInformation(
            title = detail.username,
            url = URI(url),
            description = detail.description ?: "",
            type = "Dailymotion",
            cover = cover
        )
    }

    override fun findInformation(url: String): Mono<FindPodcastInformation> {
        return findPodcastInformation(url).toMono()
    }

    override fun compatibility(url: String): Int = when {
        "www.dailymotion.com" in url -> 1
        else -> Int.MAX_VALUE
    }
}

private val USER_NAME_EXTRACTOR = "^.+dailymotion.com/(?<id>.*)".toRegex()

@JsonIgnoreProperties(ignoreUnknown = true)
private class DailymotionUserDetail(
        @JsonProperty("avatar_720_url") val avatar: String,
        val username: String,
        val description: String?
)
