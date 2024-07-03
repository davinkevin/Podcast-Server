package com.github.davinkevin.podcastserver.update.updaters.upload

import com.github.davinkevin.podcastserver.update.updaters.PodcastToUpdate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI
import java.util.*

/**
 * Created by kevin on 28/06/15 for Podcast Server
 */
@ExtendWith(SpringExtension::class)
class UploadUpdaterTest(
        @Autowired private val updater: UploadUpdater
) {

    private var podcast = PodcastToUpdate(
            url = URI("http://foo.bar.com"),
            signature = "noSign",
            id = UUID.randomUUID()
    )

    @TestConfiguration
    @Import(UploadUpdaterConfig::class)
    class LocalTestConfig

    @Test
    fun `should serve items`() {
        assertThat(updater.findItems(podcast)).isEmpty()
    }

    @Test
    fun `should generate an empty signature`() {
        assertThat(updater.signatureOf(podcast.url)).isEqualTo("")
    }

    @Test
    fun `should show his type`() {

        /* When */
        val type = updater.type()

        /* Then */
        assertThat(type.key).isEqualTo("upload")
        assertThat(type.name).isEqualTo("Upload")
    }

    @Nested
    @DisplayName("compatibility")
    inner class Compatibility {

        @Test
        fun `should not be compatible`() {
            /* Given */
            val url = "http://foo.bar.com"
            /* When */
            val compatibility = updater.compatibility(url)
            /* Then */
            assertThat(compatibility).isEqualTo(Integer.MAX_VALUE)
        }
    }
}
