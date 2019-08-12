package com.github.davinkevin.podcastserver.find

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.router

/**
 * Created by kevin on 2019-08-11
 */

@Configuration
@Import(FindHandler::class)
class FindRoutingConfig {

    @Bean
    fun findRouting(find: FindHandler) = router {
        POST("/api/v1/podcasts/find", find::find)
    }

}

@Import(FindRoutingConfig::class, FindService::class)
class FindConfig