package com.github.davinkevin.podcastserver.config

import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Created by kevin on 29/12/2021
 */
@Configuration
class JooqConfig {

    @Bean
    fun dslContext(cf: ConnectionFactory): DSLContext {
        val settings = Settings().withBindOffsetDateTimeType(true)

        return DSL.using(cf)
            .configuration()
            .derive(settings)
            .dsl()
    }
}
