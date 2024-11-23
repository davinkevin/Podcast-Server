package com.github.davinkevin.podcastserver.kodi

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.function.router

@Configuration
@Import(KodiHandler::class)
class KodiRouterConfig {

    @Bean
    fun kodiRouter(handler: KodiHandler) = router {
        GET("/kodi/", handler::podcasts)
        GET("/kodi/{podcastName}/", handler::items)
        GET("/kodi/{podcastName}/{itemTitle}", handler::item)
    }

}

@Configuration
@Import(KodiRepository::class)
class KodiConfig