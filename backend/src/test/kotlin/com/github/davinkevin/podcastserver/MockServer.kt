package com.github.davinkevin.podcastserver

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.extension.*
import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import java.net.URI

private const val port = 5555

class MockServer: BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private lateinit var server: WireMockServer

    override fun beforeEach(p0: ExtensionContext?) {
        server = WireMockServer(port)
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

fun remapRestClientToMockServer(host: String) = RestClientCustomizer { rc ->
    rc.requestInterceptor { request, body, execution ->
        val mockServerUrl = request.uri.toASCIIString()
            .replace("https", "http")
            .replace(host, "localhost:$port")
            .let(::URI)

        val modifiedRequest = request.copy(uri = mockServerUrl)
        execution.execute(modifiedRequest, body)
    }
}

internal fun HttpRequest.copy(uri: URI): HttpRequest {
    return object: HttpRequest {
        override fun getHeaders(): HttpHeaders = this@copy.headers
        override fun getMethod(): HttpMethod = this@copy.method
        override fun getURI(): URI = uri
        override fun getAttributes() = this@copy.attributes
    }
}