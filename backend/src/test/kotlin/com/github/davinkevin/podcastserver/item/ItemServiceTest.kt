package com.github.davinkevin.podcastserver.item

import com.github.davinkevin.podcastserver.service.FileService
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by kevin on 2019-02-12
 */
@ExtendWith(SpringExtension::class)
@Import(ItemService::class)
class ItemServiceTest {

    @Autowired lateinit var itemService: ItemService
    @MockBean lateinit var repository: ItemRepositoryV2
    @MockBean lateinit var p: PodcastServerParameters
    @MockBean lateinit var fileService: FileService

    @Test
    fun `should delete old items`() {
        /* Given */
        val limit = ZonedDateTime.now().minusDays(30)
        whenever(p.limitDownloadDate()).thenReturn(limit)
        val items = listOf(
                DeleteItemInformation(UUID.fromString("2e7d6cc7-c3ed-47d1-866f-7f797624124d"), "foo", "bar"),
                DeleteItemInformation(UUID.fromString("dca41d0b-a59c-43fa-8d2d-2129fb637546"), "num1", "num2"),
                DeleteItemInformation(UUID.fromString("40430ce3-b421-4c82-b34d-2deb4c46b1cd"), "itemT", "podcastT")
        )
        val repoResponse = Flux.fromIterable(items)
        whenever(repository.findAllToDelete(limit.toOffsetDateTime())).thenReturn(repoResponse)
        doNothing().whenever(fileService).deleteItem(any())
        whenever(repository.deleteById(any())).thenReturn(Mono.empty())

        /* When */
        StepVerifier.create(itemService.deleteOldEpisodes())
                .expectSubscription()
                .then {
                    val paths = items.map { i -> i.path }

                    verify(repository).findAllToDelete(limit.toOffsetDateTime())
                    verify(fileService, times(3)).deleteItem(argWhere { it in paths })
                }
                /* Then */
                .verifyComplete()
    }
}