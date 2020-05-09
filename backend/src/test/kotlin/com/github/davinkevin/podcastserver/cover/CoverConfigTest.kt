package com.github.davinkevin.podcastserver.cover

import com.github.davinkevin.podcastserver.service.FileService
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.RouterFunction
import java.time.Clock

/**
 * Created by kevin on 09/05/2020
 */
class CoverConfigTest {

    private val contextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CoverConfig::class.java,
                    LocalTestConfiguration::class.java
            ))

    @Test
    fun `should provide cover repository`() {
        /* Given */
        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(CoverRepository::class.java)
        }
    }

    @Test
    fun `should provide cover service`() {
        /* Given */
        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(CoverService::class.java)
        }
    }

    @Test
    fun `should require clock`() {
        /* Given */
        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(Clock::class.java)
        }
    }

    @Test
    fun `should provide router & handler`() {
        /* Given */
        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(RouterFunction::class.java)
            assertThat(it).hasSingleBean(CoverHandler::class.java)
        }
    }

}

@Configuration
private class LocalTestConfiguration {

    @Bean fun query(): DSLContext = mock()
    @Bean fun file(): FileService = mock()

}
