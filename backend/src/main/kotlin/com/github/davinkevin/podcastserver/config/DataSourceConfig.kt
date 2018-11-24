package com.github.davinkevin.podcastserver.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.time.ZonedDateTime.now
import java.time.temporal.TemporalAccessor
import java.util.*

/**
 * Created by kevin on 11/04/15
 */
@Configuration
@EntityScan(basePackages = ["lan.dk.podcastserver.entity", "com.github.davinkevin.podcastserver.entity"])
@ComponentScan("lan.dk.podcastserver.repository")
@EnableTransactionManagement
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
class DataSourceConfig {

    @Bean
    fun dateTimeProvider() = DateTimeProvider { Optional.of<TemporalAccessor>(now()) }
}
