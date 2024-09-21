package com.github.davinkevin.podcastserver.config

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.db.MetricsDSLContext
import io.micrometer.core.instrument.config.MeterFilter
import org.jooq.DSLContext
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class MetricsConfig {

    @Bean
    fun addApplicationPrefix(): MeterFilter = object : MeterFilter {
        override fun map(id: Meter.Id): Meter.Id {
            return id.withName("podcast-server." + id.name).withTags(id.tags)
        }
    }

    @Bean
    fun jooqMetrics(meterRegistry: ObjectProvider<MeterRegistry?>) = object : BeanPostProcessor {
        override fun postProcessAfterInitialization(bean: Any, beanName: String): Any = when (bean) {
            !is DSLContext -> bean
            else -> MetricsDSLContext.withMetrics(bean, meterRegistry.getObject(), Tags.of("query", "jooq"))
        }
    }
}