package com.github.davinkevin.podcastserver.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vavr.jackson.datatype.VavrModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Created by kevin on 15/06/2016 for Podcast Server
 */
@Configuration
class JacksonConfig {

    @Bean
    fun mapper() = ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModules(
                    Hibernate5Module()
                            .enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING)
                            .disable(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION),
                    JavaTimeModule(),
                    VavrModule(),
                    KotlinModule()
            )!!

}
