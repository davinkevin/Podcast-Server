package com.github.davinkevin.podcastserver.find

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.find.finders.ItunesFinder
import com.github.davinkevin.podcastserver.find.finders.ItunesFinderConfig
import com.github.davinkevin.podcastserver.manager.worker.rss.RSSFinder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.router
import reactor.netty.http.client.HttpClient
import java.nio.charset.Charset

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

@Configuration
@Import(
        FindRoutingConfig::class,
        FindService::class,

        ItunesFinderConfig::class
)
class FindConfig

