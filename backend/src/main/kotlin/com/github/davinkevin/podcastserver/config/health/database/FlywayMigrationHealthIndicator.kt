package com.github.davinkevin.podcastserver.config.health.database

import com.github.davinkevin.podcastserver.database.Tables.FLYWAY_SCHEMA_HISTORY
import org.jooq.DSLContext
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.ApplicationEventPublisher
import java.util.concurrent.atomic.AtomicBoolean

class FlywayMigrationHealthIndicator(
    private val dsl: DSLContext,
    private val publisher: ApplicationEventPublisher,
    private val properties: FlywayExpectedProperties,
) : HealthIndicator {

    private val ready = AtomicBoolean(false)

    override fun health(): Health {
        val expected = properties.version

        if (ready.get()) {
            return Health.up().withDetail("expected", expected).build()
        }

        val applied = runCatching { lookupAppliedVersion() }.getOrElse {
            return Health.outOfService()
                .withDetail("expected", expected)
                .withDetail("reason", "flyway_schema_history unavailable: ${it.message}")
                .build()
        }

        if (applied >= expected) {
            if (ready.compareAndSet(false, true)) {
                publisher.publishEvent(DatabaseReadyEvent(this))
            }
            return Health.up()
                .withDetail("applied", applied)
                .withDetail("expected", expected)
                .build()
        }

        return Health.outOfService()
            .withDetail("applied", applied)
            .withDetail("expected", expected)
            .build()
    }

    private fun lookupAppliedVersion(): Int = dsl
        .select(FLYWAY_SCHEMA_HISTORY.VERSION)
        .from(FLYWAY_SCHEMA_HISTORY)
        .where(FLYWAY_SCHEMA_HISTORY.SUCCESS.eq(true))
        .fetch { it.value1().toIntOrNull() ?: 0 }
        .maxOrNull() ?: 0
}
