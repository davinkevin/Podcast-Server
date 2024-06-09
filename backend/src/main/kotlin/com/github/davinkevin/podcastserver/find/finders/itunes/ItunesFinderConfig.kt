package com.github.davinkevin.podcastserver.find.finders.itunes

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.find.finders.rss.RSSFinder
import com.github.davinkevin.podcastserver.find.finders.rss.RSSFinderConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
@Import(RSSFinderConfig::class)
class ItunesFinderConfig {

    @Bean
    fun itunesFinder(om: ObjectMapper, rssFinder: RSSFinder, wcb: WebClient.Builder): ItunesFinder {
        val wc = wcb
                .clone()
                .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))
                .baseUrl("https://itunes.apple.com/")
                .build()

        return ItunesFinder(rssFinder, wc, om)
    }
}

