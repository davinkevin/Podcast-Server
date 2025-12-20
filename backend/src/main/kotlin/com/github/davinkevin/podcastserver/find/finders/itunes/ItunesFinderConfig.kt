package com.github.davinkevin.podcastserver.find.finders.itunes

import com.github.davinkevin.podcastserver.find.finders.rss.RSSFinder
import com.github.davinkevin.podcastserver.find.finders.rss.RSSFinderConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper

@Configuration
@Import(RSSFinderConfig::class)
class ItunesFinderConfig {

    @Bean
    fun itunesFinder(om: ObjectMapper, rssFinder: RSSFinder, rcb: RestClient.Builder): ItunesFinder {
        val rc = rcb
                .clone()
                .baseUrl("https://itunes.apple.com/")
                .build()

        return ItunesFinder(rssFinder, rc, om)
    }
}

