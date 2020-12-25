package com.github.davinkevin.podcastserver.messaging

import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.core.env.get
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.router


/**
 * Created by kevin on 01/05/2020
 */
@Configuration
@Import(MessageHandler::class, MessagingTemplate::class)
class MessagingRoutingConfig {

    @Bean
    fun messageRouter(message: MessageHandler) = router {
        GET("/api/v1/sse", message::sseMessages)
    }

}
