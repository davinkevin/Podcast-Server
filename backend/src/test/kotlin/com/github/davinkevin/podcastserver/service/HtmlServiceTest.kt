package com.github.davinkevin.podcastserver.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import org.jsoup.select.Elements
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
class HtmlServiceTest {

    @Spy lateinit var urlService: UrlService
    @InjectMocks lateinit var htmlService: HtmlService

    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(PORT))

    @BeforeEach
    fun beforeEach() {
        wireMockServer.start()
        WireMock.configureFor("localhost", PORT)
    }

    @AfterEach fun afterEach() = wireMockServer.stop()

    companion object {
        private const val PORT = 8089
        private const val LOCALHOST = "http://localhost:$PORT"
    }

    @Test
    fun `should reject get with empty`() {
        /* Given */
        /* When */
        val document = htmlService.get("$LOCALHOST/page/foo.html")
        /* Then */
        assertThat(document).isEmpty()
    }

    @Test
    fun `should get_page`() {
        /* Given */
        stubFor(get(urlEqualTo("/page/file.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBodyFile("service/htmlService/jsoup.html")))

        /* When */
        val document = htmlService.get("$LOCALHOST/page/file.html")

        /* Then */
        assertThat(document.isDefined).isTrue()
        assertThat(document.map { it.head().select("title").text() })
                .contains("JSOUP Example")
    }

    @Test
    fun should_parse_string() {
        /* Given */
        val html = "<div></div>"
        /* When */
        val document = htmlService.parse(html)
        /* Then */
        assertThat(document)
                .isNotNull()
                .isInstanceOf(Document::class.java)
    }


    @Test
    fun should_generate_a_collector_for_html_element() {
        /* Given */
        val divWithFooBar = Element(Tag.valueOf("div"), "foo-bar")
        val span = Element(Tag.valueOf("span"), "Text")
        val p = Element(Tag.valueOf("p"), "foo-bar")

        /* When */
        val elements = Stream
                .of(divWithFooBar, span, p)
                .collect(HtmlService.toElements())

        /* Then */
        assertThat(elements)
                .hasSize(3)
                .contains(divWithFooBar, span, p)
                .isInstanceOf(Elements::class.java)
    }
}