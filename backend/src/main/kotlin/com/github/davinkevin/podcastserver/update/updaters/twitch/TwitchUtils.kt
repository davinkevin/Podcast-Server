package com.github.davinkevin.podcastserver.update.updaters.twitch

import com.github.davinkevin.podcastserver.update.updaters.Type

internal val type = Type("Twitch", "Twitch")

/**
 * A channel is `twitch.tv/<login>`, optionally suffixed by `/videos` and whatever
 * query string the browser left there. Everything else served under the same
 * first path segment — a single vod, a clip, the directory, the settings… — is not
 * a channel.
 */
private val channelUrl =
    "^https?://(www\\.|m\\.)?twitch\\.tv/(?<login>[a-zA-Z0-9_]{1,25})(/videos)?/?(\\?.*)?$".toRegex()

/** first path segments twitch reserves for itself, which can never be a channel */
private val reservedPathSegments = setOf(
    "videos", "directory", "settings", "subscriptions", "p", "team", "teams",
    "collections", "clips", "downloads", "store", "jobs", "turbo", "prime",
    "friends", "inventory", "wallet", "drops", "products", "search", "login",
    "signup", "popout",
)

internal fun extractLogin(url: String): String? {
    val login = channelUrl.find(url)?.groups?.get("login")?.value
        ?: return null

    return login.takeUnless { it.lowercase() in reservedPathSegments }
}

internal fun twitchCompatibility(url: String): Int = when (extractLogin(url)) {
    null -> Integer.MAX_VALUE
    else -> 1
}
