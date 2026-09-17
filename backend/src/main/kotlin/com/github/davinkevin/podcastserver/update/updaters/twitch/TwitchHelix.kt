package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * Resolves a channel login to its user, the entry point of every twitch lookup:
 * the finder maps it to the podcast information, the updater only needs its id.
 */
internal fun RestClient.helixUser(token: TwitchTokenProvider, login: String): TwitchUser? = token
    .withRetryOn401 { accessToken ->
        get()
            .uri { it.path("/helix/users").queryParam("login", login).build() }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .retrieve()
            .body<TwitchUsersResponse>()
    }
    ?.data
    ?.firstOrNull()

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class TwitchUsersResponse(val data: List<TwitchUser> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class TwitchUser(
    val id: String,
    @param:JsonProperty("display_name") val displayName: String = "",
    val description: String = "",
    @param:JsonProperty("profile_image_url") val profileImageUrl: String = "",
    @param:JsonProperty("offline_image_url") val offlineImageUrl: String = "",
)
