package com.github.davinkevin.podcastserver.service.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import reactor.core.publisher.Mono


const val s3MockBackendPortForConfig = 1235

/**
 * Created by kevin on 21/01/2022
 */
class FileStorageConfigTest {

    private val context = ApplicationContextRunner()
        .withUserConfiguration(FileStorageConfig::class.java)

    @Test
    fun `should init bucket`() {
        /* Given */
        val service: FileStorageService = mock()
        whenever(service.initBucket()).thenReturn(Mono.empty())

        /* When */
        context.withPropertyValues(
            "podcastserver.storage.bucket=data",
            "podcastserver.storage.username=foo",
            "podcastserver.storage.password=bar",
            "podcastserver.storage.url=http://localhost:$s3MockBackendPortForConfig/",
        )
            .withBean(FileStorageService::class.java, { service })
            .withAllowBeanDefinitionOverriding(true)
            /* Then */
            .run {
                assertThat(it).hasSingleBean(CommandLineRunner::class.java)

                it.getBean(CommandLineRunner::class.java)
                    .run()

                verify(service, times(1)).initBucket()
            }
    }
}
