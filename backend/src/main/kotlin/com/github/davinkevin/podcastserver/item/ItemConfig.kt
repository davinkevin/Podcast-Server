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
            GET("/search", item::search)
            DELETE("/clean", item::clean)
        }

        "/api/v1/podcasts/{idPodcast}".nest {
            GET("/items", item::pocastItems )
            "/items/{id}".nest {
                GET("/", item::findById)
                GET("/cover.{ext}", item::cover)
                GET("/{file}", item::file)
                POST("/reset", item::reset)
            }
        }
    }
}

@Configuration
@Import(ItemRepositoryV2::class, ItemRoutingConfig::class, ItemService::class, ItemHandler::class)
class ItemConfig
