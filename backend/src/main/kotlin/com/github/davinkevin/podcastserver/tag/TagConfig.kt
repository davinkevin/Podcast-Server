package com.github.davinkevin.podcastserver.tag

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-03-19
 */
@Configuration
class TagRoutingConfig {

    @Bean
    fun tagRouter(tag: TagHandler) = router {
        "/api/v1/tags".nest {
            GET("/{id}", tag::findById)
        }
    }
}

@Configuration
@Import(TagRepositoryV2::class, TagRoutingConfig::class, TagService::class, TagHandler::class)
class TagConfig
