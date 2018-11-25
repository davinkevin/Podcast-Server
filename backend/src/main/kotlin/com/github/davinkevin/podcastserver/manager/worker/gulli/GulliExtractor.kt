package com.github.davinkevin.podcastserver.manager.worker.gulli

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import arrow.syntax.collections.firstOption
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.davinkevin.podcastserver.manager.downloader.DownloadingItem
import com.github.davinkevin.podcastserver.manager.worker.Extractor
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.utils.MatcherExtractor.Companion.from
import com.github.davinkevin.podcastserver.utils.k
import com.jayway.jsonpath.TypeRef
import com.github.davinkevin.podcastserver.entity.Item
import lan.dk.podcastserver.service.JsonService
import org.jsoup.nodes.Element
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.regex.Pattern

/**
 * Created by kevin on 03/12/2017
 */
@Component
@Scope("prototype")
class GulliExtractor(val htmlService: HtmlService, val jsonService: JsonService) : Extractor {

    override fun extract(item: Item) =
            htmlService.get(item.url!!).k()
                    .map { it.select("script") }
                    .flatMap { it.firstOption { e -> e.html().contains("playlist:") } }
                    .flatMap { getPlaylistFromGulliScript(it) }
                    .map { DownloadingItem(item, listOf(it), getFileName(item), null) }
                    .getOrElse { throw RuntimeException("Gulli Url extraction failed") }

    private fun getPlaylistFromGulliScript(element: Element): Option<String> {
        val playlist = PLAYLIST_EXTRACTOR.on(element.html()).group(1).k()
        val numberInPlaylist = NUMBER_IN_PLAYLIST_EXTRACTOR.on(element.html()).group(1).k().map { it.toInt() }

        return playlist
                .flatMap { jsonService.parse(it).toOption() }
                .map { JsonService.to(GULLI_ITEM_TYPE_REF).apply(it) }
                .flatMap { l -> numberInPlaylist.map { l[it] } }
                .flatMap { it.sources.firstOption { s -> s.file.contains("mp4") } }
                .map { it.file }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class GulliItem(val sources: List<GulliSource> = listOf())

    @JsonIgnoreProperties(ignoreUnknown = true)
    class GulliSource(val file: String = "")

    override fun compatibility(url: String?): Int =
            if ("replay.gulli.fr" in (url ?: "")) 1
            else Integer.MAX_VALUE

    companion object {
        private val NUMBER_IN_PLAYLIST_EXTRACTOR = from("playlistItem\\(([^)]*)\\);")
        private val PLAYLIST_EXTRACTOR = from(Pattern.compile("playlist:\\s*(.*?(?=events:))", Pattern.DOTALL))
        private val GULLI_ITEM_TYPE_REF = object : TypeRef<List<GulliItem>>() {}
    }
}