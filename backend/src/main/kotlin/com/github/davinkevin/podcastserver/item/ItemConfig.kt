package com.github.davinkevin.podcastserver.item

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-02-03
 */
@Configuration
class ItemConfig {

    @Bean
    fun itemRouter(item: ItemHandler) = router {
        "/api/v1/items".nest {
            DELETE("/clean", item::clean)
        }
    }

}