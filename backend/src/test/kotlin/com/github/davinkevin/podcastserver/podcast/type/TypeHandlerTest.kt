package com.github.davinkevin.podcastserver.podcast.type

import com.github.davinkevin.podcastserver.extension.json.assertThatJson
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector
import com.github.davinkevin.podcastserver.manager.worker.Type
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

/**
 * Created by kevin on 2019-03-03
 */
@WebFluxTest(controllers = [TypeHandler::class])
@Import(TypeRoutingConfig::class)
@ImportAutoConfiguration(ErrorWebFluxAutoConfiguration::class)
class TypeHandlerTest {

    @Autowired lateinit var rest: WebTestClient
    @MockBean lateinit var updaterSelector: UpdaterSelector

    private val dailymotion = Type("Dailymotion", "Dailymotion")
    private val franceTv = Type("FranceTv", "France•tv")
    private val gulli = Type("Gulli", "Gulli")
    private val jeuxVideoCom = Type("JeuxVideoCom", "JeuxVideo.com")
    private val myCanal = Type("MyCanal", "MyCanal")
    private val rss = Type("RSS", "RSS")
    private val sixPlay = Type("SixPlay", "6Play")
    private val tf1Replay = Type("TF1Replay", "TF1 Replay")
    private val upload = Type("upload", "Upload")
    private val youtube = Type("Youtube", "Youtube")

    @Nested
    @DisplayName("should find all")
    inner class ShouldFindAll {

        @Test
        fun `and return all element inside a wrapper`() {
            /* Given */
            val types = setOf(dailymotion, franceTv, gulli, jeuxVideoCom, myCanal, rss, sixPlay, tf1Replay, upload, youtube)
            whenever(updaterSelector.types()).thenReturn(types)

            /* When */
            rest
                    .get()
                    .uri("/api/v1/podcasts/types")
                    .exchange()
                    /* Then */
                    .expectStatus().isOk
                    .expectBody()
                    .assertThatJson { isEqualTo("""{ "content":[
                          { "key":"Dailymotion", "name":"Dailymotion" },
                          { "key":"FranceTv", "name":"France•tv" },
                          { "key":"Gulli", "name":"Gulli" },
                          { "key":"JeuxVideoCom", "name":"JeuxVideo.com" },
                          { "key":"MyCanal", "name":"MyCanal" },
                          { "key":"RSS", "name":"RSS" },
                          { "key":"SixPlay", "name":"6Play" },
                          { "key":"TF1Replay", "name":"TF1 Replay" },
                          { "key":"upload", "name":"Upload" },
                          { "key":"Youtube", "name":"Youtube" }
                    ] } """) }
        }
    }

}
