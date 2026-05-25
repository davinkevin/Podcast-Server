package com.github.davinkevin.podcastserver.config.health

import com.github.davinkevin.podcastserver.config.health.database.FlywayExpectedProperties
import com.github.davinkevin.podcastserver.config.health.database.FlywayMigrationHealthIndicator
import org.jooq.DSLContext
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:flyway-expected.properties")
@EnableConfigurationProperties(FlywayExpectedProperties::class)
class HealthConfig {

    @Bean
    fun flywayMigration(
        dsl: DSLContext,
        publisher: ApplicationEventPublisher,
        flyway: FlywayExpectedProperties,
    ): FlywayMigrationHealthIndicator = FlywayMigrationHealthIndicator(dsl, publisher, flyway)
}
