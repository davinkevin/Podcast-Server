package com.github.davinkevin.podcastserver.item

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.router
import com.github.davinkevin.podcastserver.item.ItemRepositoryV2 as ItemRepository

/**
 * Created by kevin on 2019-02-03
 */
@Configuration
@Import(ItemHandler::class)
class ItemRoutingConfig {

    @Bean
    fun itemRouter(item: ItemHandler) = router {
        "/api/v1/items".nest {
            GET("/search", item::search)
            DELETE("/clean", item::clean)
        }

        "/api/v1/podcasts/{idPodcast}".nest {
            GET("/items", item::pocastItems )
            POST("/items/upload", item::upload)
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
@Import(ItemRepository::class, ItemRoutingConfig::class, ItemService::class)
class ItemConfig {

    @Bean
    fun onStartupCleanInvalidStateItems(item: ItemRepository) = CommandLineRunner {
        item
                .resetItemWithDownloadingState()
                .block()
    }

}
