package com.github.davinkevin.podcastserver.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

/**
 * Created by kevin on 15/06/2016 for Podcast Server
 */
@Configuration
class JacksonConfig {

    @Bean
    fun mapperCustomization() = Jackson2ObjectMapperBuilderCustomizer {
        it.featuresToDisable(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
        )
            .serializerByType(Path::class.java, ToStringSerializer())
            .modules(
                JavaTimeModule(),
                kotlinModule()
            )
    }

}
