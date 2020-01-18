package com.github.davinkevin.podcastserver.find

import com.github.davinkevin.podcastserver.manager.worker.Finder
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.kotlin.core.publisher.toMono
import reactor.test.StepVerifier
import java.net.URI

@ExtendWith(SpringExtension::class)
@Import(FindService::class)
class FindServiceTest(
        @Autowired private val service: FindService
) {

    @MockBean(name = "firstFinder") private lateinit var firstFinder: Finder
    @MockBean(name = "secondFinder") private lateinit var secondFinder: Finder

    @Test
    fun `should find most compatible finder and delegate to it the find operation`() {
        /* Given */
        val url = "https://foo.bar.com"
        val p = FindPodcastInformation(title = "", url = URI(url), type = "first", cover = null, description = "")
        whenever(firstFinder.compatibility(url)).thenReturn(2)
        whenever(firstFinder.findInformation(url)).thenReturn(p.toMono())
        whenever(secondFinder.compatibility(url)).thenReturn(3)
        /* When */
        StepVerifier.create(service.find(URI(url)))
                /* Then */
                .expectSubscription()
                .expectNext(p)
                .verifyComplete()
    }

    @Test
    fun `should fallback on default response if the selected finder crash during find operation`() {
        /* Given */
        val url = "https://foo.bar.com"
        whenever(firstFinder.compatibility(url)).thenReturn(2)
        whenever(firstFinder.findInformation(url)).thenReturn(RuntimeException("error !").toMono())
        whenever(secondFinder.compatibility(url)).thenReturn(3)
        /* When */
        StepVerifier.create(service.find(URI(url)))
                /* Then */
                .expectSubscription()
                .expectNext(FindPodcastInformation(title = "", url = URI(url), type = "RSS", cover = null, description = ""))
                .verifyComplete()
    }
}
