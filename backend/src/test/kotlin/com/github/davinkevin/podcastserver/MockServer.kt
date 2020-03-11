package com.github.davinkevin.podcastserver

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.extension.*
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import reactor.kotlin.core.publisher.toMono
import java.net.URI

class MockServer: BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private lateinit var server: WireMockServer

    override fun beforeEach(p0: ExtensionContext?) {
        server = WireMockServer(5555)
        server.start()
    }

    override fun afterEach(context: ExtensionContext?) {
        server.stop()
        server.resetAll()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == WireMockServer::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext): Any {
        return server
    }
}

fun remapToMockServer(host: String) = WebClientCustomizer { it.filter(ExchangeFilterFunction.ofRequestProcessor { c ->
    val mockServerUrl = c.url().toASCIIString()
            .replace("https", "http")
            .replace(host, "localhost:5555")

    ClientRequest.from(c)
            .url(URI(mockServerUrl))
            .build()
            .toMono()
}) }
