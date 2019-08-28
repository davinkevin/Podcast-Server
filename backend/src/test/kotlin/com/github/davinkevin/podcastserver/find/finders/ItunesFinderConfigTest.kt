package com.github.davinkevin.podcastserver.find.finders

import com.github.davinkevin.podcastserver.service.ImageService
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class ItunesFinderConfigTest {

    private val contextRunner = ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ItunesFinderConfig::class.java, MockDependenciesConfig::class.java))

    @Test
    fun `should have rest client configured with itunes domain`() {
        /* Given */
        /* When */
        contextRunner.run {
            /* Then */
            assertThat(it).hasSingleBean(ItunesFinder::class.java)
        }
    }


}

@Configuration
@AutoConfigureWebClient
class MockDependenciesConfig {
    @Bean fun mockImageServer() = mock<ImageService>()
}
