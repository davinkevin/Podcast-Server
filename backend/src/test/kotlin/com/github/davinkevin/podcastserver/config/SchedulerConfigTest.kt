package com.github.davinkevin.podcastserver.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class SchedulerConfigTest {

    @InjectMocks lateinit var schedulerConfig: SchedulerConfig

    @Test
    fun should_have_config_injected() {
        assertThat(schedulerConfig).isNotNull
    }

    @Test
    fun should_have_spring_annotations() {
        assertThat(schedulerConfig.javaClass)
                .hasAnnotations(Configuration::class.java, EnableScheduling::class.java)
    }
}
