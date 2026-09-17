package com.github.davinkevin.podcastserver.update.updaters.twitch

import org.springframework.boot.context.properties.ConfigurationProperties

internal val acceptedVideoTypes = setOf("all", "archive", "highlight", "upload")

@ConfigurationProperties("podcastserver.api.twitch")
data class TwitchApi(
    val clientId: String = "",
    val clientSecret: String = "",
    val videoTypes: List<String> = listOf("all"),
) {

    init {
        val invalid = videoTypes
            .map { it.trim().lowercase() }
            .filterNot { it in acceptedVideoTypes }

        require(invalid.isEmpty()) {
            "invalid value(s) ${invalid.joinToString(", ")} for podcastserver.api.twitch.video-types, " +
                "accepted values are ${acceptedVideoTypes.joinToString(", ")}"
        }
    }

    /**
     * Types to ask helix for, one call each. A `null` entry means "no `type`
     * parameter at all": `all` is documented as the default of that filter, so
     * omitting it yields the same result without relying on `all` being accepted
     * as an explicit value.
     */
    val helixVideoTypes: List<String?> by lazy {
        val normalized = videoTypes.map { it.trim().lowercase() }.distinct()

        when {
            normalized.isEmpty() -> listOf(null)
            "all" in normalized -> listOf(null)
            else -> normalized
        }
    }
}
