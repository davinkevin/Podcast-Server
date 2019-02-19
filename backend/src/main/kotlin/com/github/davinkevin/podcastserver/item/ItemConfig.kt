package com.github.davinkevin.podcastserver.item

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-02-03
 */
@Configuration
class ItemRoutingConfig {

    @Bean
    fun itemRouter(item: ItemHandler) = router {
        "/api/v1/items".nest {
            DELETE("/clean", item::clean)
        }
    }

    @Bean
    fun podcastItemRouter(item: ItemHandler) = router {
        GET("/api/v1/podcasts/{idPodcast}/items/{id}", item::findById)
        GET("/api/v1/podcasts/{idPodcast}/items/{id}/cover.{ext}", item::cover)
        GET("/api/v1/podcasts/{idPodcast}/items/{id}/{file}", item::file)
    }
}

@Configuration
@Import(ItemRepositoryV2::class, ItemRoutingConfig::class, ItemService::class, ItemHandler::class)
class ItemConfig
