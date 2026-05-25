package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.config.health.database.DatabaseReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.event.EventListener
import org.springframework.web.servlet.function.router

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
            DELETE("", item::clean)
        }

        "/api/v1/podcasts/{idPodcast}".nest {
            GET("/items", item::podcastItems)
            POST("/items/upload", item::upload)
            "/items/{id}".nest {
                GET("", item::findById)
                GET("/cover.{ext}", item::cover)
                GET("/playlists", item::playlists)
                GET("/{file}", item::file)
                POST("/reset", item::reset)
                DELETE("", item::delete)
            }
        }
    }

}

@Configuration
@Import(ItemRepository::class, ItemRoutingConfig::class, ItemService::class, OnDatabaseReadyItemCleaner::class)
class ItemConfig

class OnDatabaseReadyItemCleaner(private val item: ItemRepository) {
    @EventListener
    fun onDatabaseReady(@Suppress("UNUSED_PARAMETER") event: DatabaseReadyEvent) {
        item.resetItemWithDownloadingState()
    }
}
