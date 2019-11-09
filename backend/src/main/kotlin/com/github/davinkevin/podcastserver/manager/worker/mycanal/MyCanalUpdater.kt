package com.github.davinkevin.podcastserver.manager.worker.mycanal

import arrow.core.*
import arrow.syntax.collections.firstOption
import com.github.davinkevin.podcastserver.manager.worker.*
import com.github.davinkevin.podcastserver.service.HtmlService
import com.github.davinkevin.podcastserver.service.ImageService
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.utils.toTry
import com.jayway.jsonpath.TypeRef
import lan.dk.podcastserver.service.JsonService
import lan.dk.podcastserver.service.JsonService.to
import org.jsoup.select.Elements
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.net.URI

@Component
@Scope(SCOPE_PROTOTYPE)
class MyCanalUpdater(val signatureService: SignatureService, val jsonService: JsonService, val imageService: ImageService, val htmlService: HtmlService) : Updater {

    override fun blockingFindItems(podcast: PodcastToUpdate): Set<ItemFromUpdate> =
            itemsAsJsonFrom(URI(podcast.url.toASCIIString()))
                    .getOrElse { setOf() }
                    .flatMap { findDetails(it).toList() }
                    .map { toItem(it.second, DOMAIN + it.first.onClick.path) }
                    .toSet()

    override fun blockingSignatureOf(url: URI): String {
        return itemsAsJsonFrom(url)
                .map { items -> items.map { it.contentID }.sorted().joinToString(",") }
                .map { signatureService.fromText(it) }
                .getOrElse { throw RuntimeException("Error during signature of podcast with url " + url.toASCIIString()) }
    }

    private fun itemsAsJsonFrom(url: URI) =
            htmlService.get(url.toASCIIString())
                    .map { it.body() }
                    .map { it.select("script") }
                    .getOrElse { Elements() }
                    .asSequence()
                    .map { it.html() }
                    .firstOption { "__data" in it }
                    .flatMap { extractJsonConfig(it).toOption() }
                    .map { jsonService.parse(it) }
                    .map { to("landing.strates[*].contents[*]", SET_OF_MY_CANAL_ITEM).apply(it) }

    private fun findDetails(item: MyCanalItem): Option<Pair<MyCanalItem, MyCanalDetailsItem>> {
        val json = jsonService.parseUrl(URL_DETAILS.format(item.contentID)).toTry()

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

    private fun toItem(i: MyCanalDetailsItem, url: String): ItemFromUpdate {
        val infos = i.infos
        val titrage = infos.titrage
        val publication = infos.publication

        return ItemFromUpdate(
            title = titrage.titre,
            description = titrage.sous_titre,
            length = i.duration,
            cover = i.media.images.cover().map { imageService.fetchCoverInformation(it) }.getOrElse { null }?.toCoverFromUpdate(),
            pubDate = publication.asZonedDateTime(),
            url = URI(url)
        )
    }

    companion object {

        private val SET_OF_MY_CANAL_ITEM = object : TypeRef<Set<MyCanalItem>>() {}
        private val SET_OF_MY_CANAL_DETAILS_ITEM = object : TypeRef<Set<MyCanalDetailsItem>>() {}

        private const val URL_DETAILS = "https://secure-service.canal-plus.com/video/rest/getVideosLiees/cplus/%s?format=json"
        private const val DOMAIN = "https://www.mycanal.fr"
    }
}
