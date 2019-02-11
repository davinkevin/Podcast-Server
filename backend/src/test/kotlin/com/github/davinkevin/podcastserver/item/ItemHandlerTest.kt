package com.github.davinkevin.podcastserver.item

import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

/**
 * Created by kevin on 2019-02-12
 */
@WebFluxTest(controllers = [ItemHandler::class])
@Import(ItemConfig::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class ItemHandlerTest {

    @Autowired lateinit var rest: WebTestClient
    @MockBean lateinit var itemService: ItemService

    @Nested
    @DisplayName("should delete")
    inner class ShouldDelete {

        @Test
        fun `and returns ok`() {
            /* Given */
            whenever(itemService.deleteOldEpisodes()).thenReturn(Mono.empty())

            /* When */
            rest.delete()
                    .uri("/api/v1/items/clean")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
        }


    }

}