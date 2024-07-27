package com.github.davinkevin.podcastserver.config

import org.apache.coyote.ProtocolHandler
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
class TomcatConfig {

    @Bean
    fun protocolHandlerVirtualThreadExecutorCustomizer() = TomcatProtocolHandlerCustomizer {
        proto: ProtocolHandler -> proto.executor = Executors.newVirtualThreadPerTaskExecutor()
    }
}
