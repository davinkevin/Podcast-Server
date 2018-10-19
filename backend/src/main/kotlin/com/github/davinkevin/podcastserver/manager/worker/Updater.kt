package com.github.davinkevin.podcastserver.manager.worker

import io.vavr.API.Set
import io.vavr.API.Tuple
import io.vavr.Tuple
import io.vavr.Tuple3
import io.vavr.collection.Set
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Podcast
import org.slf4j.LoggerFactory
import java.util.function.Predicate

interface Updater {

    fun update(podcast: Podcast): Tuple3<Podcast, Set<Item>, Predicate<Item>> {
        return try {
            val signature = signatureOf(podcast)
            if (signature == podcast.signature) {
                log.info(""""{}" hasn't change""", podcast.title)
                return NO_MODIFICATION_TUPLE
            }
            podcast.signature = signature
            Tuple.of(podcast, getItems(podcast), notIn(podcast))
        } catch (e: Exception) {
            log.info(""""{}" triggered the following error during update""", podcast.title, e)
            NO_MODIFICATION_TUPLE
        }
    }

    fun getItems(podcast: Podcast): Set<Item>

    fun signatureOf(podcast: Podcast): String

    fun notIn(podcast: Podcast): Predicate<Item> = Predicate{ item -> !podcast.contains(item) }

    fun type(): Type

    fun compatibility(url: String?): Int

    companion object {
        val log = LoggerFactory.getLogger(Updater::class.java)
        val NO_MODIFICATION_TUPLE = Tuple<Podcast, Set<Item>, Predicate<Item>>(Podcast.DEFAULT_PODCAST, Set(), Predicate{ true })!!
    }
}
