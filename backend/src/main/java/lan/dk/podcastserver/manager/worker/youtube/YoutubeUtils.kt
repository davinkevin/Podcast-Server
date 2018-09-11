package lan.dk.podcastserver.manager.worker.youtube

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.k
import java.util.*

/**
 * Created by kevin on 13/09/2018
 */

internal fun playlistIdOf(url: String): String {
    // TODO  : Use Pattern Match to extract PlaylistID in Feed case and url case
    return url.substringAfter("list=")
}

internal fun isPlaylist(url: String) = url.contains(PLAYLIST_URL_PART)
internal const val PLAYLIST_URL_PART = "www.youtube.com/playlist?list="

internal fun channelIdOf(htmlService: HtmlService, url: String) =
        htmlService
                .get(url).k()
                .flatMap { it.select("[data-channel-external-id]").firstOption() }
                .filter { Objects.nonNull(it) }
                .map { it.attr("data-channel-external-id") }
                .getOrElse { "" }

