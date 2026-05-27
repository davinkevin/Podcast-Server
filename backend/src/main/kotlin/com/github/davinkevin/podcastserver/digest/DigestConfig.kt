package com.github.davinkevin.podcastserver.digest

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.function.router

@Configuration
@Import(DigestHandler::class)
class DigestRoutingConfig {

    @Bean
    fun digestRouter(digest: DigestHandler) = router {
        "/api/v1/digest".nest {
            GET("", digest::find)
        }
    }
}

@Configuration
@Import(DigestRoutingConfig::class, DigestRepository::class, DigestService::class)
class DigestConfig
