package com.github.davinkevin.podcastserver.find.finders.twitch

import com.github.davinkevin.podcastserver.find.FindPodcastInformation
import com.github.davinkevin.podcastserver.find.finders.Finder
import com.github.davinkevin.podcastserver.find.finders.fetchFindCoverInformation
import com.github.davinkevin.podcastserver.service.image.ImageService
import com.github.davinkevin.podcastserver.update.updaters.twitch.TwitchTokenProvider
import com.github.davinkevin.podcastserver.update.updaters.twitch.extractLogin
import com.github.davinkevin.podcastserver.update.updaters.twitch.helixUser
import com.github.davinkevin.podcastserver.update.updaters.twitch.twitchCompatibility
import org.springframework.web.client.RestClient
import java.net.URI

/**
 * Twitch is a single page application which filters non-browser user agents, so the
 * usual og-tag scraping doesn't apply — the channel is looked up through helix, like
 * the updater does.
 */
class TwitchFinder(
    private val rc: RestClient,
    private val token: TwitchTokenProvider,
    private val image: ImageService,
) : Finder {

    override fun findPodcastInformation(url: String): FindPodcastInformation? {
        val login = extractLogin(url)
            ?: return null

        val user = rc.helixUser(token, login)
            ?: return null

        val cover = listOf(user.profileImageUrl, user.offlineImageUrl)
            .firstOrNull { it.isNotBlank() }
            ?.let { image.fetchFindCoverInformation(URI(it)) }

        return FindPodcastInformation(
            title = user.displayName,
            description = user.description,
            url = URI(url),
            cover = cover,
            type = "Twitch",
        )
    }

    override fun compatibility(url: String): Int = twitchCompatibility(url)
}
