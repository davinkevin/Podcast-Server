package com.github.davinkevin.podcastserver.messaging

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
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
