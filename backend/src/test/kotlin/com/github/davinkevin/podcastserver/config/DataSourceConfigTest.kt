package com.github.davinkevin.podcastserver.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.context.annotation.Configuration
import java.time.ZonedDateTime.from
import java.time.ZonedDateTime.now

/**
 * Created by kevin on 13/08/15 for Podcast Server
 */
class DataSourceConfigTest {

    private val config = DataSourceConfig()

    @Test
    fun should_be_configuration() {
        assertThat(DataSourceConfig::class.java).hasAnnotation(Configuration::class.java)
    }

    @Test
    fun should_have_a_now_date_time_provider() {
        /* Given */
        val dateTimeProvider = config.dateTimeProvider()
        /* When */
        val now = dateTimeProvider.now
        /* Then */
        assertThat(now.map { from(it) }.get()).isBeforeOrEqualTo(now())
    }
}
