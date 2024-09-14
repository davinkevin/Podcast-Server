package com.github.davinkevin.podcastserver.update.updaters

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
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
            itemProducer = { emptyList() },
            signatureProducer = { podcast.signature },
            registry = SimpleMeterRegistry()
        )

        /* When */
        val updatePodcastInformation = updater.update(podcast)

        /* Then */
        assertThat(updatePodcastInformation).isNull()
    }

    @Test
    fun `should update the podcast`() {
        /* Given */
        val podcast = PodcastToUpdate(
            id = UUID.fromString("ca735a0a-19eb-41a6-ae2e-13a7f253da6f"),
            url = URI("http://podacst.domain.com/foo/bar"),
            signature = "qfeijeqoijvoiqjnveoiqjvoij="
        )
        val items = listOf(
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
            itemProducer = { items },
            signatureProducer = { "qefokijqeiojqoiejeqf=" },
            registry = SimpleMeterRegistry(),
        )

        /* When */
        val updatePodcastInformation = updater.update(podcast)

        /* Then */
        assertThat(updatePodcastInformation).isNotNull
            .isEqualTo(UpdatePodcastInformation(podcast, items.toSet(), "qefokijqeiojqoiejeqf="))
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
            itemProducer = { error("findItem ends in error…") },
            signatureProducer = { "qefokijqeiojqoiejeqf=" },
            registry = mock(),
        )

        /* When */
        val updatePodcastInformation = updater.update(podcast)

        /* Then */
        assertThat(updatePodcastInformation).isNull()
    }
}

private val fixedDate = Clock.fixed(OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"))

class SimpleUpdater(
    private val itemProducer: (podcast: PodcastToUpdate) -> List<ItemFromUpdate>,
    private val signatureProducer: (url: URI) -> String,
    override val registry: MeterRegistry,
) : Updater {
    override fun findItems(podcast: PodcastToUpdate): List<ItemFromUpdate> = itemProducer.invoke(podcast)
    override fun signatureOf(url: URI): String = signatureProducer.invoke(url)
    override fun type() = Type("Foo", "Bar")
    override fun compatibility(url: String): Int = -1
}
