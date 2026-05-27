package com.github.davinkevin.podcastserver.digest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime

class DigestServiceTest {

    private val repository = mock<DigestRepository>()
    private val service = DigestService(repository)

    @Test
    fun `delegates to the repository`() {
        // given
        val since = OffsetDateTime.parse("2019-02-25T05:06:07Z")
        val expected = emptyList<DigestPodcast>()
        whenever(repository.findActivePodcastsSince(since, 12)).thenReturn(expected)

        // when
        val result = service.findActivePodcastsSince(since, 12)

        // then
        assertThat(result).isSameAs(expected)
        verify(repository).findActivePodcastsSince(since, 12)
    }
}
