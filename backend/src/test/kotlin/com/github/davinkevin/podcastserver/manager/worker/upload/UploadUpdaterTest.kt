package com.github.davinkevin.podcastserver.manager.worker.upload

import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.worker.PodcastToUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.*

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
class UploadUpdaterTest {

    private var podcast = PodcastToUpdate (
            url = URI("http://foo.bar.com"),
            signature = "noSign",
            id = UUID.randomUUID()
    )

    val updater = UploadUpdater()

    @Test
    fun `should serve items`() {
        assertThat(updater.findItems(podcast)).isEmpty()
    }

    @Test
    fun `should generate an empty signature`() {
        assertThat(updater.signatureOf(podcast.url)).isEmpty()
    }

    @Test
    fun `should show his type`() {

        /* When */
        val type = updater.type()

        /* Then */
        assertThat(type).isSameAs(UploadUpdater.TYPE)
        assertThat(type.key).isEqualTo("upload")
        assertThat(type.name).isEqualTo("Upload")
    }

    @Test
    fun `should not be compatible`() {
        assertThat(updater.compatibility("http://foo/bar")).isEqualTo(Integer.MAX_VALUE)
    }
}
