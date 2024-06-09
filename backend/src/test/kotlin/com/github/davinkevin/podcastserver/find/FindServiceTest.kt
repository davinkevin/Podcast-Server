package com.github.davinkevin.podcastserver.find

import com.github.davinkevin.podcastserver.find.finders.Finder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.kotlin.core.publisher.toMono
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
        whenever(firstFinder.findPodcastInformation(url)).thenReturn(p)
        whenever(secondFinder.compatibility(url)).thenReturn(3)

        /* When */
        val podcastInfo = service.find(URI(url))

        /* Then */
        assertThat(podcastInfo).isNotNull()
            .isSameAs(p)
    }

    @Test
    fun `should fallback on default response if the selected finder crash during find operation`() {
        /* Given */
        val url = "https://foo.bar.com"
        whenever(firstFinder.compatibility(url)).thenReturn(2)
        whenever(firstFinder.findPodcastInformation(url)).thenReturn(null)
        whenever(secondFinder.compatibility(url)).thenReturn(3)

        /* When */
        val podcastInfo = service.find(URI(url))

        /* Then */
        assertThat(podcastInfo).isEqualTo(
            FindPodcastInformation(title = "", url = URI(url), type = "RSS", cover = null, description = "")
        )
    }
}
