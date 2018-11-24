package com.github.davinkevin.podcastserver.manager.worker.mycanal

import arrow.core.Option
import arrow.core.Try
import arrow.core.getOrElse
import arrow.core.orElse
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.manager.worker.Type
import com.github.davinkevin.podcastserver.manager.worker.Updater
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.k
import com.github.davinkevin.podcastserver.utils.toTry
import com.jayway.jsonpath.TypeRef
import com.github.davinkevin.podcastserver.entity.Cover
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import lan.dk.podcastserver.service.JsonService
import lan.dk.podcastserver.service.JsonService.to
import org.jsoup.select.Elements
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(SCOPE_PROTOTYPE)
class MyCanalUpdater(val signatureService: SignatureService, val jsonService: JsonService, val imageService: ImageService, val htmlService: HtmlService) : Updater {

    override fun findItems(podcast: Podcast) =
            itemsAsJsonFrom(podcast)
                    .getOrElse { setOf() }
                    .flatMap { findDetails(it).toList() }
                    .map { it.first to toItem(it.second) }
                    .map { it.second.apply { url = DOMAIN + it.first.onClick.path } }
                    .toSet()

    override fun signatureOf(podcast: Podcast): String {
        return itemsAsJsonFrom(podcast)
                .map { items -> items.map { it.contentID }.sorted().joinToString(",") }
                .map { signatureService.fromText(it) }
                .getOrElse { throw RuntimeException("Error during signature of " + podcast.title + " with url " + podcast.url) }
    }

    private fun itemsAsJsonFrom(p: Podcast) =
            htmlService.get(p.url).k()
                    .map { it.body() }
                    .map { it.select("script") }
                    .getOrElse { Elements() }
                    .asSequence()
                    .map { it.html() }
                    .firstOption { "__data" in it }
                    .flatMap { extractJsonConfig(it).k() }
                    .map { jsonService.parse(it) }
                    .map { to("landing.strates[*].contents[*]", SET_OF_MY_CANAL_ITEM).apply(it) }

    private fun findDetails(item: MyCanalItem): Option<Pair<MyCanalItem, MyCanalDetailsItem>> {
        val json = jsonService.parseUrl(URL_DETAILS.format(item.contentID)).k().toTry()

        val fromCollection = {
            json
                    .flatMap { Try { JsonService.to(SET_OF_MY_CANAL_DETAILS_ITEM).apply(it) } }
                    .getOrElse { setOf() }
                    .firstOption { it.id == item.contentID }
        }

        return json
                .flatMap { Try { JsonService.to(MyCanalDetailsItem::class.java).apply(it) } }
                .toOption()
                .orElse(fromCollection)
                .map { item to it }

    }

    override fun type() = Type("MyCanal", "MyCanal")

    override fun compatibility(url: String?) = myCanalCompatibility(url)

    private fun toItem(i: MyCanalDetailsItem): Item {
        val infos = i.infos
        val titrage = infos.titrage
        val publication = infos.publication

        return Item().apply {
            title = titrage.titre
            description = titrage.sous_titre
            length = i.duration
            cover = i.media.images.cover().map { imageService.getCoverFromURL(it) }.getOrElse { Cover.DEFAULT_COVER }
            pubDate = publication.asZonedDateTime()
        }
    }

    companion object {

        private val SET_OF_MY_CANAL_ITEM = object : TypeRef<Set<MyCanalItem>>() {}
        private val SET_OF_MY_CANAL_DETAILS_ITEM = object : TypeRef<Set<MyCanalDetailsItem>>() {}

        private const val URL_DETAILS = "https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/%s?format=json"
        private const val DOMAIN = "https://www.mycanal.fr"
    }
}
