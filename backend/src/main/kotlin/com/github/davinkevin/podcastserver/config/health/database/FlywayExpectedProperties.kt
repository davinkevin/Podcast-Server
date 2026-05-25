package com.github.davinkevin.podcastserver.config.health.database

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("flyway.expected")
data class FlywayExpectedProperties(
    val version: Int,
)
