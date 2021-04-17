package com.github.davinkevin.podcastserver.tag

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-03-19
 */
@Configuration
@Import(TagHandler::class)
class TagRoutingConfig {

    @Bean
    fun tagRouter(tag: TagHandler) = coRouter {
        "/api/v1/tags".nest {
            GET("search", tag::findByNameLike)
            GET("{id}", tag::findById)
        }
    }

}

@Configuration
@Import(
        TagRepository::class,
        TagRoutingConfig::class,
        TagService::class
)
class TagConfig
