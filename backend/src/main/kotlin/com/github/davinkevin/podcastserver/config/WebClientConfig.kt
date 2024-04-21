package com.github.davinkevin.podcastserver.config

import org.apache.coyote.ProtocolHandler
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeStrategies
import java.util.concurrent.Executors


/**
 * Created by kevin on 15/12/2019
 */
@Configuration
class WebClientConfig {

    @Bean
    fun increaseMemorySizeOfWebClient() = WebClientCustomizer { wcb ->
        wcb.exchangeStrategies(ExchangeStrategies.builder().codecs { it.defaultCodecs().maxInMemorySize(1024 * 10 * 10 * 10 * 10) }.build())
    }

    @Bean
    fun protocolHandlerVirtualThreadExecutorCustomizer() = TomcatProtocolHandlerCustomizer {
        proto: ProtocolHandler -> proto.executor = Executors.newVirtualThreadPerTaskExecutor()
    }
}
