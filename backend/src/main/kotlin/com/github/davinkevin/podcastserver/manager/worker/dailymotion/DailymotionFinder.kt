package com.github.davinkevin.podcastserver.manager.worker.dailymotion

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.utils.MatcherExtractor.Companion.from
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.Finder
import lan.dk.podcastserver.service.JsonService
import org.springframework.stereotype.Service
import javax.validation.constraints.NotEmpty

/**
 * Created by kevin on 23/02/2016 for Podcast Server
 */
@Service
class DailymotionFinder(val jsonService: JsonService, val imageService: ImageService) : Finder {

    // http://www.dailymotion.com/karimdebbache
    override fun find(url: String) =
            USER_NAME_EXTRACTOR.on(url).group(1)
                    .map { API_URL.format(it) }
                    .flatMap { jsonService.parseUrl(it) }
                    .map { JsonService.to(DailymotionUserDetail::class.java).apply(it) }
                    .map { jsonToPodcast(url, it) }
                    .getOrElse { Podcast.DEFAULT_PODCAST }

    private fun jsonToPodcast(anUrl: String, detail: DailymotionUserDetail) =
            Podcast().apply {
                url = anUrl
                title = detail.username
                description = detail.description
                type = "Dailymotion"
                cover = imageService.getCoverFromURL(detail.avatar)
            }

    override fun compatibility(@NotEmpty url: String?) =
            if (url != null && url.contains("www.dailymotion.com")) 1
            else Integer.MAX_VALUE

    companion object {
        private const val API_URL = "https://api.dailymotion.com/user/%s?fields=avatar_720_url,description,username"
        private val USER_NAME_EXTRACTOR = from("^.+dailymotion.com/(.*)")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class DailymotionUserDetail(
        @JsonProperty("avatar_720_url") val avatar: String,
        val username: String,
        val description: String
)
