package com.github.davinkevin.podcastserver.controller;

import com.github.davinkevin.podcastserver.business.ItemBusiness
import com.github.davinkevin.podcastserver.business.WatchListBusiness
import com.github.davinkevin.podcastserver.config.JacksonConfig
import com.github.davinkevin.podcastserver.config.WebFluxConfig
import com.github.davinkevin.podcastserver.entity.Cover
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import com.nhaarman.mockitokotlin2.whenever
import lan.dk.podcastserver.controller.api.ItemController
import net.javacrumbs.jsonunit.assertj.JsonAssert
import net.javacrumbs.jsonunit.assertj.JsonAssertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@WebFluxTest(controllers = [(ItemController::class)])
@ContextConfiguration(classes = [WebFluxConfig::class])
@Import(ItemController::class, JacksonConfig::class)
@ImportAutoConfiguration(value = [ErrorWebFluxAutoConfiguration::class])
class ItemControllerTest {

    @Autowired lateinit var rest: WebTestClient
    @MockBean lateinit var itemBusiness: ItemBusiness
    @MockBean lateinit var idm: ItemDownloadManager
    @MockBean lateinit var watchListBusiness: WatchListBusiness

    @Nested
    @DisplayName("should find by id")
    inner class ShouldFindById {

        val itemId = UUID.fromString("904ee60c-c510-4172-8a09-2ec0ed25d442")
        val coverId = UUID.fromString("39fed2d8-1401-40ad-b367-e9ce612461aa")
        val podcastId = UUID.fromString("d352d583-83cc-4c09-910a-ae5d5d8adad7")

        @Test
        fun `an return item `() {
            /* Given */
            val p = Podcast().apply { id = podcastId }
            val c = Cover().apply { id = coverId; url = "http://foo.bar.com/url/to/cover.png"; width = 100; height = 100 }
            val item = Item().apply {
                id = itemId
                cover = c
                podcast = p
                title = "a custom podcast !"

                creationDate = ZonedDateTime.of(2019, 1, 14, 14, 15, 16, 17, ZoneId.of("Europe/Paris"))
                pubDate = ZonedDateTime.of(2019, 1, 15, 14, 15, 16, 17, ZoneId.of("Europe/Paris"))
                downloadDate = ZonedDateTime.of(2019, 1, 16, 14, 15, 16, 17, ZoneId.of("Europe/Paris"))

                description = "foo bar !"
                fileName = "a_simple_filename.mp3"
            }
            whenever(itemBusiness.findOne(itemId)).thenReturn(item)

            /* When */
            rest.get()
                    .uri("/api/podcasts/$podcastId/items/$itemId")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson {
                        isEqualTo("""{
                                   "cover":{
                                      "height":100,
                                      "id":"39fed2d8-1401-40ad-b367-e9ce612461aa",
                                      "url":"/api/v1/podcasts/d352d583-83cc-4c09-910a-ae5d5d8adad7/items/904ee60c-c510-4172-8a09-2ec0ed25d442/cover.png",
                                      "width":100
                                   },
                                   "creationDate":"2019-01-14T14:15:16.000000017+01:00",
                                   "pubDate":"2019-01-15T14:15:16.000000017+01:00",
                                   "downloadDate":"2019-01-16T14:15:16.000000017+01:00",
                                   "description":"foo bar !",
                                   "fileName":"a_simple_filename.mp3",
                                   "id":"904ee60c-c510-4172-8a09-2ec0ed25d442",
                                   "isDownloaded":true,
                                   "length":null,
                                   "mimeType":null,
                                   "podcastId":"d352d583-83cc-4c09-910a-ae5d5d8adad7",
                                   "progression":0,
                                   "proxyURL":"/api/v1/podcasts/d352d583-83cc-4c09-910a-ae5d5d8adad7/items/904ee60c-c510-4172-8a09-2ec0ed25d442/a_custom_podcast__.mp3",
                                   "status":"NOT_DOWNLOADED",
                                   "title":"a custom podcast !",
                                   "url":null
                                }""")
                    }
        }

    }
}

fun WebTestClient.BodyContentSpec.assertThatJson( t: JsonAssert.ConfigurableJsonAssert.() -> Unit ): WebTestClient.BodyContentSpec {
    val json = String(returnResult().responseBody!!)
    t(JsonAssertions.assertThatJson(json))
    return this
}
