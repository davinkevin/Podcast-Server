package com.github.davinkevin.podcastserver.config

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.ToStringSerializer
import tools.jackson.module.kotlin.kotlinModule
import java.nio.file.Path

/**
 * Created by kevin on 15/06/2016 for Podcast Server
 */
@Configuration
class JacksonConfig {

    @Bean
    fun mapperCustomization() = JsonMapperBuilderCustomizer {
        it
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .addModules(
                SimpleModule("PathToString").apply { addSerializer(Path::class.java, ToStringSerializer.instance) },
                kotlinModule(),
            )
    }

}
