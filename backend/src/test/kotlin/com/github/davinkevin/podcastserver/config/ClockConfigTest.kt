package com.github.davinkevin.podcastserver.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Created by kevin on 09/05/2020
 */
@ExtendWith(SpringExtension::class)
@Import(ClockConfig::class)
class ClockConfigTest(
        @Autowired private val clock: Clock
) {

    @Test
    fun `should provide clock sync on system`() {
        /* Given */
        val now = ZonedDateTime.now()
        /* When */
        val nowOnClock = ZonedDateTime.now(clock)
        /* Then */
        assertThat(nowOnClock).isEqualToIgnoringNanos(now)
    }

}
