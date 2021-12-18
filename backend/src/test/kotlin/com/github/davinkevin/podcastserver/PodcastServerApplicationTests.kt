package com.github.davinkevin.podcastserver

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.*
import org.springframework.boot.test.context.SpringBootTest

/**
 * Created by kevin on 13/03/2020
 */
@SpringBootTest(
    properties = [
        "podcastserver.externaltools.ffmpeg=/bin/echo",
        "podcastserver.externaltools.ffprobe=/bin/echo",
        "podcastserver.storage.url=http://localhost:9000/"
    ]
)
@ExtendWith(MockS3Server::class)
@EnabledOnOs(OS.LINUX, OS.MAC)
class PodcastServerApplicationTests {
    @Test fun contextLoads() {}
}

private class MockS3Server: BeforeAllCallback, AfterAllCallback {

    private lateinit var server: WireMockServer

    override fun beforeAll(p0: ExtensionContext?) {
        server = WireMockServer(9000).apply {
            start()
            stubFor(head(urlEqualTo("/data")).willReturn(ok()))
            stubFor(put(urlEqualTo("/data?policy")).willReturn(ok()))
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        server.stop()
    }
}
