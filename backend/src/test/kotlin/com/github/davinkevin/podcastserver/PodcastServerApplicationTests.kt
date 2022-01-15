package com.github.davinkevin.podcastserver

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.boot.test.context.SpringBootTest

/**
 * Created by kevin on 13/03/2020
 */
@SpringBootTest(
    properties = [
        "podcastserver.externaltools.ffmpeg=/bin/echo",
        "podcastserver.externaltools.ffprobe=/bin/echo",
        "podcastserver.storage.url=http://localhost:9000/",
        "podcastserver.storage.bucket=bucket",
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
            stubFor(head(urlEqualTo("/bucket")).willReturn(ok()))
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        server.stop()
    }
}
