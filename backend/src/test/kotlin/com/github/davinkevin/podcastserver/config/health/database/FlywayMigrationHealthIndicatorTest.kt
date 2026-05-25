package com.github.davinkevin.podcastserver.config.health.database

import com.github.davinkevin.podcastserver.extension.spring.NestedSpringTest
import com.github.davinkevin.podcastserver.database.Tables.FLYWAY_SCHEMA_HISTORY
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.health.contributor.Status
import org.springframework.boot.jooq.test.autoconfigure.JooqTest
import org.springframework.context.ApplicationEventPublisher

@JooqTest
@NestedSpringTest
class FlywayMigrationHealthIndicatorTest(
    @Autowired private val dsl: DSLContext,
) {

    private val publisher: ApplicationEventPublisher = mock()
    private val appliedVersion: Int by lazy { highestAppliedVersion() }

    @Test
    fun `returns UP when applied version equals expected`() {
        val indicator = FlywayMigrationHealthIndicator(dsl, publisher, properties = FlywayExpectedProperties(version = appliedVersion))

        val health = indicator.health()

        assertThat(health.status).isEqualTo(Status.UP)
        assertThat(health.details).containsEntry("applied", appliedVersion)
        assertThat(health.details).containsEntry("expected", appliedVersion)
        verify(publisher).publishEvent(any<DatabaseReadyEvent>())
    }

    @Test
    fun `returns UP when applied version is greater than expected`() {
        val indicator = FlywayMigrationHealthIndicator(dsl, publisher, properties = FlywayExpectedProperties(version = 1))

        val health = indicator.health()

        assertThat(health.status).isEqualTo(Status.UP)
        verify(publisher).publishEvent(any<DatabaseReadyEvent>())
    }

    @Test
    fun `returns OUT_OF_SERVICE when applied version is lower than expected`() {
        val indicator = FlywayMigrationHealthIndicator(dsl, publisher, properties = FlywayExpectedProperties(version = 999))

        val health = indicator.health()

        assertThat(health.status).isEqualTo(Status.OUT_OF_SERVICE)
        assertThat(health.details).containsEntry("applied", appliedVersion)
        assertThat(health.details).containsEntry("expected", 999)
        verify(publisher, never()).publishEvent(any<DatabaseReadyEvent>())
    }

    @Test
    fun `publishes DatabaseReadyEvent only once across repeated health checks`() {
        val indicator = FlywayMigrationHealthIndicator(dsl, publisher, properties = FlywayExpectedProperties(version = appliedVersion))

        repeat(5) { indicator.health() }

        verify(publisher, times(1)).publishEvent(any<DatabaseReadyEvent>())
    }

    @Test
    fun `returns OUT_OF_SERVICE when flyway_schema_history is unavailable`() {
        val brokenDsl: DSLContext = mock {
            on { select(any<org.jooq.Field<*>>()) } doThrow DataAccessException("relation does not exist")
        }
        val indicator = FlywayMigrationHealthIndicator(brokenDsl, publisher, properties = FlywayExpectedProperties(version = 10))

        val health = indicator.health()

        assertThat(health.status).isEqualTo(Status.OUT_OF_SERVICE)
        assertThat(health.details).containsEntry("expected", 10)
        assertThat(health.details).containsKey("reason")
        verify(publisher, never()).publishEvent(any<DatabaseReadyEvent>())
    }

    private fun highestAppliedVersion(): Int = dsl
        .select(FLYWAY_SCHEMA_HISTORY.VERSION)
        .from(FLYWAY_SCHEMA_HISTORY)
        .where(FLYWAY_SCHEMA_HISTORY.SUCCESS.eq(true))
        .fetch { it.value1().toIntOrNull() ?: 0 }
        .max()
}
