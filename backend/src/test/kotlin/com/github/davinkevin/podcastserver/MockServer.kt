package com.github.davinkevin.podcastserver

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.extension.*
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import reactor.kotlin.core.publisher.toMono
import java.net.URI

class MockServer: BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private lateinit var server: WireMockServer

    override fun beforeAll(context: ExtensionContext) {
        server = WireMockServer(5555)
        server.start()
    }

    override fun afterAll(context: ExtensionContext?) {
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

fun remapToMockServer(host: String) = ExchangeFilterFunction.ofRequestProcessor { c ->
    val mockServerUrl = c.url().toASCIIString()
            .replace("https", "http")
            .replace(host, "localhost:5555")

    ClientRequest.from(c)
            .url(URI(mockServerUrl))
            .build()
            .toMono()
}
