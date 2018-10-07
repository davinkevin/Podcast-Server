package com.github.davinkevin.podcastserver.service.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Created by kevin on 13/04/2016 for Podcast Server
 */
class ApiTest {

    @Test
    fun should_have_default_value() {
        /* Given */
        /* When */
        val api = Api()
        /* Then */

        assertThat(api.youtube).isEqualTo(null)
        assertThat(api.dailymotion).isEqualTo(null)
    }

    @Test
    fun should_have_specified_values() {
        /* Given */
        val youtubeKey = "YoutubeKey"
        val dailymotionKey = "dailymotionKey"

        /* When */
        val api = Api(
                dailymotion = dailymotionKey,
                youtube = youtubeKey
        )

        /* Then */
        assertThat(api.youtube).isEqualTo(youtubeKey)
        assertThat(api.dailymotion).isEqualTo(dailymotionKey)
    }

}
