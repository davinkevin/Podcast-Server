package com.github.davinkevin.podcastserver.manager.worker.youtube

import arrow.core.getOrElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.k
import io.vavr.collection.Stream
import lan.dk.podcastserver.manager.worker.Type
import java.util.*

/**
 * Created by kevin on 13/09/2018
 */

internal const val PLAYLIST_URL_PART = "www.youtube.com/playlist?list="

internal fun playlistIdOf(url: String): String = url.substringAfter("list=")
internal fun isPlaylist(url: String) = url.contains(PLAYLIST_URL_PART)

internal fun channelIdOf(htmlService: HtmlService, url: String) =
        htmlService
                .get(url).k()
                .flatMap { it.select("[data-channel-external-id]").firstOption() }
                .filter { Objects.nonNull(it) }
                .map { it.attr("data-channel-external-id") }
                .getOrElse { "" }

internal fun isYoutubeUrl(url: String?) =
        Stream
                .of("youtube.com/channel/", "youtube.com/user/", "youtube.com/", "gdata.youtube.com/feeds/api/playlists/")
                .any { url?.contains(it) == true }

const val YOUTUBE = "Youtube"
internal fun _type() = Type(YOUTUBE, YOUTUBE)
internal fun _compatibility(url: String?) =
        if (isYoutubeUrl(url)) 1
        else Integer.MAX_VALUE

