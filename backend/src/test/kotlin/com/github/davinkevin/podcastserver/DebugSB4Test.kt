package com.github.davinkevin.podcastserver

import com.github.davinkevin.podcastserver.entity.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@Configuration
class ExternalConfiguration {
    @Bean
    fun state() = Status.FINISH
}

@ExtendWith(SpringExtension::class)
@Import(ExternalConfiguration::class)
class DebugSB4Test(
    @Autowired private val status: Status
) {

    @Nested
    @DisplayName("inside")
    inner class Inside {

        @Test
        fun `should be ok`() {
            /* Given */
            /* When */
            /* Then */
            assertThat(true).isTrue()
            assertThat(status).isEqualTo(Status.FINISH)
        }
    }
}
