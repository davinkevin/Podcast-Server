package com.github.davinkevin.podcastserver.business

import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Tag
import com.github.davinkevin.podcastserver.service.JdomService
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.*
import com.github.davinkevin.podcastserver.entity.Podcast
import lan.dk.podcastserver.repository.PodcastRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.util.FileSystemUtils
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Created by kevin on 27/07/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class PodcastBusinessTest {

    val workingFolder = Paths.get("/tmp", "PodcastBusinessTest")

    @Mock lateinit var podcastRepository: PodcastRepository
    @InjectMocks lateinit var podcastBusiness: PodcastBusiness

    @BeforeEach
    fun beforeEach() {
        FileSystemUtils.deleteRecursively(workingFolder.toFile())
    }

    @Test
    fun should_delete() {
        /* Given */
        val podcastId = UUID.randomUUID()

        /* When */
        podcastBusiness.delete(podcastId)

        /* Then */
        verify(podcastRepository, times(1)).deleteById(podcastId)
    }
}
