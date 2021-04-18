package com.github.davinkevin.podcastserver.podcast.type

import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.router

@Configuration
@AutoConfigureOrder(0)
@Import(TypeHandler::class)
class TypeRoutingConfig {

    @Bean
    fun typeRouter(type: TypeHandler) = coRouter {
        GET("/api/v1/podcasts/types", type::findAll)
    }
}

@Configuration
@Import(
        TypeRoutingConfig::class,
        UpdaterSelector::class
)
class TypeConfig
