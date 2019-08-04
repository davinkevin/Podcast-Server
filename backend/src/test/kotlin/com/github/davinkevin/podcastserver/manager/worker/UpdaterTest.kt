package com.github.davinkevin.podcastserver.manager.worker

import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.manager.worker.SimpleUpdater.Companion.ERROR_UUID
import com.github.davinkevin.podcastserver.service.SignatureService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.entity.Podcast
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URI
import java.util.UUID
import javax.validation.Validator
import kotlin.collections.HashSet

/**
 * Created by kevin on 22/06/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class UpdaterTest {

    @Mock private lateinit var podcastServerParameters: PodcastServerParameters
    @Mock private lateinit var signatureService: SignatureService
    @Mock private lateinit var validator: Validator
    @InjectMocks private lateinit var simpleUpdater: SimpleUpdater

    @Test
    fun should_not_update_because_of_same_signature() {
        /* Given */
        val podcast = Podcast().apply {
            id = UUID.randomUUID()
            signature = "123456789"
        }

        /* When */
        val noChangeResult = simpleUpdater.update(podcast)

        /* Then */
        assertThat(noChangeResult)
                .isSameAs(Updater.NO_MODIFICATION)
        assertThat(noChangeResult.p(Item())).isEqualTo(true)
    }

    @Test
    fun should_update_the_podcast() {
        /* Given */
        val podcast = Podcast().apply {
            url = "http://foo.bar.com"
            id = UUID.randomUUID()
            signature = "XYZ"
        }

        /* When */
        val result = simpleUpdater.update(podcast)

        /* Then */
        assertThat(result).isNotSameAs(Updater.NO_MODIFICATION)
        assertThat(result.podcast).isSameAs(podcast)
        assertThat(result.items).isInstanceOf(HashSet::class.java).hasSize(3)
        assertThat(result.p).isNotNull
        assertThat(result.podcast.signature).isEqualTo("123456789")
    }

    @Test
    fun should_handle_exception_during_update() {
        /* Given */
        val podcast = Podcast().apply {
            id = ERROR_UUID
            signature = "XYZ"
        }

        /* When */
        val result = simpleUpdater.update(podcast)

        /* Then */
        assertThat(result).isSameAs(Updater.NO_MODIFICATION)
    }

    @Test
    fun should_filter_with_default_predicate() {
        /* Given */
        val item = Item().apply { id = UUID.fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82") }
        val podcast = Podcast().apply {
            id = UUID.randomUUID()
            url = "http://a.fake.url/rss.xml"
            items = mutableSetOf()
            add(item)
        }

        /* When */
        val result = simpleUpdater.update(podcast)
        val collectedItem = result.items.filter(result.p)

        /* Then */
        assertThat(collectedItem).hasSize(2)
    }

    @AfterEach
    fun afterEach() {
        verifyNoMoreInteractions(podcastServerParameters, signatureService, validator)
    }
}

private class SimpleUpdater : Updater {

    override fun findItems(podcast: Podcast) = setOf(
            Item().apply { id = UUID.fromString("214be5e3-a9e0-4814-8ee1-c9b7986bac82") },
            Item().apply { id = UUID.randomUUID() },
            Item().apply { id = UUID.randomUUID() }
    )

    override fun signatureOf(url: URI) = when {
        url === URI("http://foo.bar") -> throw RuntimeException()
        else -> "123456789"
    }

    override fun type() = Type("Foo", "Bar")
    override fun compatibility(url: String?) = -1

    companion object {
        val ERROR_UUID = UUID.randomUUID()!!
    }
}
