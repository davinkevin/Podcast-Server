package com.github.davinkevin.podcastserver.config

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeStrategies

/**
 * Created by kevin on 15/12/2019
 */
@Configuration
class WebClientConfig {

    @Bean
    fun increaseMemorySizeOfWebClient() = WebClientCustomizer { wcb ->
        wcb.exchangeStrategies(ExchangeStrategies.builder().codecs { it.defaultCodecs().maxInMemorySize(1024 * 10 * 10 * 10 * 10) }.build())
    }

}
