package com.github.davinkevin.podcastserver

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.extension.*

class MockServer: BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private lateinit var server: WireMockServer

    override fun beforeAll(context: ExtensionContext) {
        server = WireMockServer(5555)
        server.start()
    }

    override fun afterAll(context: ExtensionContext?) {
        server.stop()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == WireMockServer::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext): Any {
        return server
    }


}
