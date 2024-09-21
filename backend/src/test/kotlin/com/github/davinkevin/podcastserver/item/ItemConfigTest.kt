package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.podcast.PodcastRepository
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.context.annotation.UserConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ItemConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(UserConfigurations.of(ItemDependencyMockConfig::class.java, ItemConfig::class.java))

    @Test
    fun `should trigger a reset of all item with a downloading status (started or paused) `() {
        /* Given */
        /* When */
        contextRunner
            /* Then */
            .withConfiguration(UserConfigurations.of(MockForResetAtStartupConfig::class.java))
            .run {
                assertThat(it).hasSingleBean(CommandLineRunner::class.java)

                val repo = it.getBean(ItemRepository::class.java)
                val clr = it.getBean(CommandLineRunner::class.java)
                clr.run()


                verify(repo).resetItemWithDownloadingState()
            }
    }
}

class MockForResetAtStartupConfig {
    @Bean @Primary fun mockItemRepository(): ItemRepository = mock<ItemRepository>().apply {
        doNothing().whenever(this).resetItemWithDownloadingState()
    }
}

class ItemDependencyMockConfig {
    @Bean @Primary fun mockJOOQ(): DSLContext = mock()
    @Bean @Primary fun mockItemService(): ItemService = mock()
    @Bean @Primary fun mockFileService(): FileStorageService = mock()
    @Bean @Primary fun mockPodcastRepository(): PodcastRepository = mock()
    @Bean @Primary fun mockIDM(): ItemDownloadManager = mock()
    @Bean @Primary fun mockPodcastProps(): PodcastServerParameters = mock()
    @Bean @Primary fun fixedClock(): Clock = Clock.fixed(fixedDate.toInstant(), ZoneId.of("UTC"))
    @Bean @Primary fun registry(): MeterRegistry = SimpleMeterRegistry()
}

private val fixedDate = OffsetDateTime.of(2019, 3, 4, 5, 6, 7, 0, ZoneOffset.UTC)
