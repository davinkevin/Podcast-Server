package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.IOUtils
import com.github.davinkevin.podcastserver.entity.*
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.*
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URISyntaxException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by kevin on 08/09/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class JdomServiceTest {

    @Mock lateinit var urlService: UrlService
    @InjectMocks lateinit var jdomService: JdomService

    val wireMockServer: WireMockServer = WireMockServer(wireMockConfig().port(8282))

    @BeforeEach
    fun beforeEach() {
        wireMockServer.start()
        WireMock.configureFor("localhost", 8282)
    }

    @AfterEach
    fun afterEach() = wireMockServer.stop()

    @Test
    fun `should parse`() {
        /* Given */
        val url = "http://localhost:8282/a/valid.xml"
        whenever(urlService.asStream(anyString())).then { i -> IOUtils.urlAsStream(i.getArgument(0)) }
        stubFor(get(urlEqualTo("/a/valid.xml"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBodyFile("service/jdomService/valid.xml")))

        /* When */
        val document = jdomService.parse(url)

        /* Then */
        assertThat(document.isDefined).isTrue()
        verify(urlService, only()).asStream(url)
    }
}
