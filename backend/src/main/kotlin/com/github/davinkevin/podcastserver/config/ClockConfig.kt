package com.github.davinkevin.podcastserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {

    @Bean fun clock(): Clock = Clock.systemDefaultZone()

}
