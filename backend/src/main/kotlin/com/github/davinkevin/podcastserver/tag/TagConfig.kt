package com.github.davinkevin.podcastserver.tag

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.function.router

/**
 * Created by kevin on 2019-03-19
 */
@Configuration
@Import(TagHandler::class)
class TagRoutingConfig {
    @Bean
    fun tagRouter(tag: TagHandler) = router {
        "/api/v1/tags".nest {
            GET("/search", tag::findByNameLike)
            GET("/{id}", tag::findById)
        }
    }
}

@Configuration
@Import(
        TagRepository::class,
        TagRoutingConfig::class,
        TagService::class,
)
class TagConfig
