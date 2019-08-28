package com.github.davinkevin.podcastserver.find.finders

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.davinkevin.podcastserver.manager.worker.rss.RSSFinder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.nio.charset.Charset

@Configuration
@Import(RSSFinder::class)
class ItunesFinderConfig {

    @Bean
    fun itunesFinder(om: ObjectMapper, rssFinder: RSSFinder, wcb: WebClient.Builder): ItunesFinder {
        val wc = wcb
                .clone()
                .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect { _, res -> res.status().code() in 300..399 }))
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs { c -> c.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(om, TEXT_JAVASCRIPT_UTF8)) }
                                .build()
                )
                .baseUrl("https://itunes.apple.com/")
                .build()

        return ItunesFinder(rssFinder, wc)
    }
}

private val TEXT_JAVASCRIPT_UTF8 = MediaType("text", "javascript", Charset.forName("utf-8"))
