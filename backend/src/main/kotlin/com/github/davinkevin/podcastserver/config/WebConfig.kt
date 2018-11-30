package com.github.davinkevin.podcastserver.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
@ComponentScan("com.github.davinkevin.podcastserver.controller")
class WebFluxConfig(private val om: ObjectMapper) : WebFluxConfigurer {
    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(om))
        configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(om))
    }
}
