package com.github.davinkevin.podcastserver.extension.slf4j

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Logger as LogbackLogger

class LoggerTest {

    private lateinit var log: LogbackLogger
    private lateinit var appender: ListAppender<ILoggingEvent>

    @BeforeEach
    fun setup() {
        val ctx = LoggerFactory.getILoggerFactory() as LoggerContext
        log = ctx.getLogger("test-logger-${System.nanoTime()}")
        appender = ListAppender<ILoggingEvent>().apply {
            context = ctx
            start()
        }
        log.addAppender(appender)
        log.isAdditive = false
    }

    @AfterEach
    fun cleanup() {
        appender.stop()
        log.detachAppender(appender)
    }

    @Nested
    inner class `when debug is enabled` {

        @BeforeEach
        fun enableDebug() {
            log.level = Level.DEBUG
        }

        @Test
        fun `includes throwable in the error log`() {
            /* Given */
            val cause = RuntimeException("boom")

            /* When */
            log.errorWithDebugStack("error during {} ({})", "download", 42, throwable = cause)

            /* Then */
            assertThat(appender.list).hasSize(1)
            val event = appender.list[0]
            assertThat(event.level).isEqualTo(Level.ERROR)
            assertThat(event.formattedMessage).isEqualTo("error during download (42)")
            assertThat(event.throwableProxy).isNotNull
            assertThat(event.throwableProxy.message).isEqualTo("boom")
        }

        @Test
        fun `omits throwable when it is null`() {
            /* When */
            log.errorWithDebugStack("error during {}", "download", throwable = null)

            /* Then */
            assertThat(appender.list).hasSize(1)
            val event = appender.list[0]
            assertThat(event.level).isEqualTo(Level.ERROR)
            assertThat(event.formattedMessage).isEqualTo("error during download")
            assertThat(event.throwableProxy).isNull()
        }
    }

    @Nested
    inner class `when debug is disabled` {

        @BeforeEach
        fun disableDebug() {
            log.level = Level.INFO
        }

        @Test
        fun `omits throwable from the error log even when provided`() {
            /* Given */
            val cause = RuntimeException("boom")

            /* When */
            log.errorWithDebugStack("error during {} ({})", "download", 42, throwable = cause)

            /* Then */
            assertThat(appender.list).hasSize(1)
            val event = appender.list[0]
            assertThat(event.level).isEqualTo(Level.ERROR)
            assertThat(event.formattedMessage).isEqualTo("error during download (42)")
            assertThat(event.throwableProxy).isNull()
        }

        @Test
        fun `omits throwable when it is null`() {
            /* When */
            log.errorWithDebugStack("error during {}", "download", throwable = null)

            /* Then */
            assertThat(appender.list).hasSize(1)
            val event = appender.list[0]
            assertThat(event.level).isEqualTo(Level.ERROR)
            assertThat(event.formattedMessage).isEqualTo("error during download")
            assertThat(event.throwableProxy).isNull()
        }
    }
}
