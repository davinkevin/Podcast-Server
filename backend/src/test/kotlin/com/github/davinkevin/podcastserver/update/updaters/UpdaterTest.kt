package com.github.davinkevin.podcastserver.update.updaters

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI
import java.time.*
import java.util.*

/**
 * Created by kevin on 22/06/15 for Podcast Server
 */
class UpdaterTest {

    @Test
    fun `should not update because signature is the same`() {
        /* Given */
        val podcast = PodcastToUpdate(
                id = UUID.fromString("ca735a0a-19eb-41a6-ae2e-13a7f253da6f"),
                url = URI("http://podacst.domain.com/foo/bar"),
                signature = "qfeijeqoijvoiqjnveoiqjvoij="
        )
        val updater = SimpleUpdater(
                itemProducer = { Flux.empty() },
                signatureProducer = { podcast.signature.toMono() }
        )

        /* When */
        StepVerifier.create(updater.update(podcast))
                /* Then */
                .expectSubscription()
                .verifyComplete()
    }

    @Test
    fun `should update the podcast`() {
        /* Given */
        val podcast = PodcastToUpdate(
                id = UUID.fromString("ca735a0a-19eb-41a6-ae2e-13a7f253da6f"),
                url = URI("http://podacst.domain.com/foo/bar"),
                signature = "qfeijeqoijvoiqjnveoiqjvoij="
        )
        val items = setOf(
                ItemFromUpdate(
                        title = "title",
                        pubDate = ZonedDateTime.now(fixedDate),
                        length = 1234,
                        mimeType = "audio/mp3",
                        url = URI("http://localhost:1234/item/1"),
                        description = "desc",
                        cover = ItemFromUpdate.Cover(100, 100, URI("http://localhost:1234/item/1.png"))
                ),
                ItemFromUpdate(
                        title = "title",
                        pubDate = ZonedDateTime.now(fixedDate),
                        length = 1234,
                        mimeType = "audio/mp3",
                        url = URI("http://localhost:1234/item/2"),
                        description = "desc",
                        cover = ItemFromUpdate.Cover(100, 100, URI("http://localhost:1234/item/2.png"))
                )
        )
        val updater = SimpleUpdater(
                itemProducer = { items.toFlux() },
                signatureProducer = { "qefokijqeiojqoiejeqf=".toMono() }
        )

        /* When */
        StepVerifier.create(updater.update(podcast))
                /* Then */
                .expectSubscription()
                .expectNext(UpdatePodcastInformation(podcast, items, "qefokijqeiojqoiejeqf="))
                .verifyComplete()
    }

    @Test
    fun `should handle exception during update`() {
        /* Given */
        val podcast = PodcastToUpdate(
                id = UUID.fromString("ca735a0a-19eb-41a6-ae2e-13a7f253da6f"),
                url = URI("http://podacst.domain.com/foo/bar"),
                signature = "qfeijeqoijvoiqjnveoiqjvoij="
        )
        val updater = SimpleUpdater(
                itemProducer = { Flux.error(IllegalStateException("findItem ends in error…")) },
                signatureProducer = { "qefokijqeiojqoiejeqf=".toMono() }
        )

        /* When */
        StepVerifier.create(updater.update(podcast))
                /* Then */
                .expectSubscription()
                .verifyComplete()
    }
}

private val fixedDate = Clock.fixed(OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"))

class SimpleUpdater(
        private val itemProducer: (podcast: PodcastToUpdate) -> Flux<ItemFromUpdate>,
        private val signatureProducer: (url: URI) -> Mono<String>
) : Updater {
    override fun findItems(podcast: PodcastToUpdate): Flux<ItemFromUpdate> = itemProducer.invoke(podcast)
    override fun signatureOf(url: URI): Mono<String> = signatureProducer.invoke(url)
    override fun type() = Type("Foo", "Bar")
    override fun compatibility(url: String): Int = -1
}
