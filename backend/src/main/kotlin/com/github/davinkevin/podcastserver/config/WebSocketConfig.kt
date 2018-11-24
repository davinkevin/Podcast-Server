package com.github.davinkevin.podcastserver.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(c: MessageBrokerRegistry) {
        c
                .setApplicationDestinationPrefixes("/app")
                .enableSimpleBroker("/topic")
    }

    override fun registerStompEndpoints(r: StompEndpointRegistry) {
        r.apply {
            addEndpoint("/ws/sockjs").setAllowedOrigins("*").withSockJS()
            addEndpoint("/ws").setAllowedOrigins("*")
        }
    }

}
