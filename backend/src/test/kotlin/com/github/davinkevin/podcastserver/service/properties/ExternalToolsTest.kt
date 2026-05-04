package com.github.davinkevin.podcastserver.service.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Created by kevin on 13/04/2016 for Podcast Server
 */
class ExternalToolsTest {

    @Test
    fun should_have_default_value() {
        /* Given */
        /* When */
        val externalTools = ExternalTools()
        /* Then */
        assertThat(externalTools.ffmpeg).isEqualTo("/usr/local/bin/ffmpeg")
    }

    @Test
    fun should_change_value() {
        /* Given */
        /* When */
        val externalTools = ExternalTools(
                ffmpeg = "/tmp/ffmpeg",
        )
        /* Then */
        assertThat(externalTools.ffmpeg).isEqualTo("/tmp/ffmpeg")
    }
}
