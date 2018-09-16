package lan.dk.podcastserver.service


import com.github.davinkevin.podcastserver.service.UrlService
import com.github.davinkevin.podcastserver.service.UrlService.Companion.USER_AGENT_DESKTOP
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.function.Consumer
import java.util.stream.Collectors.joining

/**
 * Created by kevin on 22/07/2016.
 */
internal class UrlServiceTest {

    val wireMockServer: WireMockServer = WireMockServer(wireMockConfig().port(PORT))

    private val urlService: UrlService = UrlService()

    @BeforeEach
    fun beforeEach() {
        wireMockServer.start()
        WireMock.configureFor("localhost", PORT)
    }

    @AfterEach fun afterEach() = wireMockServer.stop()

    @Test
    fun `should execute a get request`() {
        /* Given */
        val url = "http://a.custom.url/foo/bar"

        /* When */
        val getRequest = urlService.get(url)

        /* Then */
        with(getRequest) {
            assertThat(this).isNotNull()
            assertThat(url).isEqualTo(url)
        }
    }

    @Test
    fun `should execute a post request`() {
        /* Given */
        val url = "http://a.custom.url/foo/bar"

        /* When */
        val postRequest = urlService.post(url)

        /* Then */
        with(postRequest) {
            assertThat(this).isNotNull()
            assertThat(url).isEqualTo(url)
        }
    }

    @Test
    fun `should get real url after redirection`() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", host("/my/ressources2.m3u8"))
        doRedirection("/my/ressources2.m3u8", host("/my/ressources3.m3u8"))
        stubFor(get(urlEqualTo("/my/ressources3.m3u8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")))

        /* When */
        val lastUrl = urlService.getRealURL(host("/my/ressources1.m3u8"))

        /* Then */
        assertThat(lastUrl).isEqualTo(host("/my/ressources3.m3u8"))
    }

    @Test
    fun `should get real url after redirection with user agent`() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", host("/my/ressources2.m3u8"))
        doRedirection("/my/ressources2.m3u8", host("/my/ressources3.m3u8"))
        stubFor(get(urlEqualTo("/my/ressources3.m3u8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")))

        /* When */
        val lastUrl = urlService.getRealURL(host("/my/ressources1.m3u8"), Consumer{ c -> c.setRequestProperty("User-Agent", USER_AGENT_DESKTOP) })
        /* Then */
        assertThat(lastUrl).isEqualTo(host("/my/ressources3.m3u8"))
    }

    @Test
    fun `should handle redirection relative to the host`() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", "own/ressources2.m3u8")
        doRedirection("/my/own/ressources2.m3u8", host("/my/ressources3.m3u8"))
        doRedirection("/my/ressources3.m3u8", "/my/ressources4.m3u8")
        stubFor(get(urlEqualTo("/my/ressources4.m3u8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")))

        /* When */
        val lastUrl = urlService.getRealURL(host("/my/ressources1.m3u8"), Consumer{ c -> c.setRequestProperty("User-Agent", USER_AGENT_DESKTOP) })
        /* Then */
        assertThat(lastUrl).isEqualTo(host("/my/ressources4.m3u8"))
    }

    @Test
    fun `should handle redirection relative to the path`() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", "own/ressources2.m3u8")
        doRedirection("/my/own/ressources2.m3u8", "wonderful/ressources3.m3u8")
        stubFor(get(urlEqualTo("/my/ressources3.m3u8"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")))

        /* When */
        val lastUrl = urlService.getRealURL(host("/my/ressources1.m3u8"), Consumer{ c -> c.setRequestProperty("User-Agent", USER_AGENT_DESKTOP) })
        /* Then */
        assertThat(lastUrl).isEqualTo(host("/my/own/wonderful/ressources3.m3u8"))
    }

    @Test
    fun `should reject after too many redirection`() {
        /* Given */
        doRedirection("/my/ressources1.m3u8", host("/my/ressources2.m3u8"))
        doRedirection("/my/ressources2.m3u8", host("/my/ressources3.m3u8"))
        doRedirection("/my/ressources3.m3u8", host("/my/ressources4.m3u8"))
        doRedirection("/my/ressources4.m3u8", host("/my/ressources5.m3u8"))
        doRedirection("/my/ressources5.m3u8", host("/my/ressources6.m3u8"))
        doRedirection("/my/ressources6.m3u8", host("/my/ressources7.m3u8"))
        doRedirection("/my/ressources7.m3u8", host("/my/ressources8.m3u8"))
        doRedirection("/my/ressources8.m3u8", host("/my/ressources9.m3u8"))
        doRedirection("/my/ressources9.m3u8", host("/my/ressources10.m3u8"))
        doRedirection("/my/ressources10.m3u8", host("/my/ressources11.m3u8"))

        /* When */ assertThatThrownBy { urlService.getRealURL(host("/my/ressources1.m3u8")) }
                .hasMessage("Too many redirects")
    }

    @Test
    fun `should get url as reader`() {
        /* Given */
        stubFor(get(urlEqualTo("/file.txt"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/x-mpegURL")
                        .withBody("A body for testing")
                )
        )

        /* When */
        val br = urlService.asReader("$HTTP_LOCALHOST/file.txt")

        /* Then */
        assertThat(br.lines().collect(joining()))
                .isEqualTo("A body for testing")
    }

    @Test
    fun `should not add protocol to url`() {
        /* GIVEN */
        val protocol = "http:"
        val url = "http://foo.bar.com/"

        /* WHEN  */
        val result = UrlService.addProtocolIfNecessary(protocol, url)

        /* THEN  */
        assertThat(result).isSameAs(url)
    }

    @Test
    fun `should add protocol to url`() {
        /* GIVEN */
        val protocol = "https:"
        val url = "//foo.bar.com/"

        /* WHEN  */
        val result = UrlService.addProtocolIfNecessary(protocol, url)

        /* THEN  */
        assertThat(result)
                .isNotSameAs(url)
                .contains("https://")
                .contains("//foo.bar.com/")
                .isEqualTo(protocol + url)
    }

    private fun doRedirection(mockUrl: String, redirectionUrl: String) {
        stubFor(
                get(urlEqualTo(mockUrl))
                        .willReturn(aResponse()
                                .withStatus(301)
                                .withHeader("Location", redirectionUrl))
        )
    }

    companion object {

        private const val PORT = 8089
        private const val HTTP_LOCALHOST = "http://localhost:$PORT"

        private fun host(path: String): String {
            return HTTP_LOCALHOST + path
        }
    }
}
