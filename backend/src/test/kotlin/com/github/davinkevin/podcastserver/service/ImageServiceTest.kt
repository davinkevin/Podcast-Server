package com.github.davinkevin.podcastserver.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension


/**
 * Created by kevin on 22/07/2018
 */
@ExtendWith(MockitoExtension::class)
class ImageServiceTest {

    val wireMockServer: WireMockServer = WireMockServer(wireMockConfig().port(PORT))
    @Spy lateinit var urlService: UrlService
    @InjectMocks lateinit var imageService: ImageService

    @BeforeEach
    fun beforeEach() {
        wireMockServer.start()
        WireMock.configureFor("localhost", PORT)
    }

    @AfterEach fun afterEach() = wireMockServer.stop()

    @Test
    fun `should return null or empty if no url`() {
        /* Given */
        val url = ""

        /* When */
        val coverFromURL = imageService.getCoverFromURL(url)

        /* Then */
        assertThat(coverFromURL).isEqualTo(null)
    }

    @Test
    fun `should retrieve information about image`() {
        /* Given */
        val imagePath = "/img/image.png"

        /* When */
        val cover = imageService.getCoverFromURL(HTTP_LOCALHOST + imagePath)!!

        /* Then */
        assertThat(cover.width).isEqualTo(256)
        assertThat(cover.height).isEqualTo(300)
        assertThat(cover.url).isEqualTo(HTTP_LOCALHOST + imagePath)
    }


    @Test
    fun `should return null if remote url not working or throw error`() {
        /* Given */
        val url = "$HTTP_LOCALHOST/img/image.png"
        whenever(urlService.asStream(url)).then { throw RuntimeException("Backend Error !") }

        /* When */
        val cover = imageService.getCoverFromURL(url)

        /* Then */
        assertThat(cover).isEqualTo(null)
    }

    @Test
    fun `should throw exception if url not valid`() {
        assertThat(imageService.getCoverFromURL("blabla")).isEqualTo(null)
    }

    companion object {
        private const val PORT = 8089
        private const val HTTP_LOCALHOST = "http://localhost:$PORT"
    }

}