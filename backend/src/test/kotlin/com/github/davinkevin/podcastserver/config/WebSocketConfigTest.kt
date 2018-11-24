package com.github.davinkevin.podcastserver.config

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration

/**
 * Created by kevin on 12/08/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class WebSocketConfigTest {

    @Mock lateinit var config: MessageBrokerRegistry
    @Mock lateinit var registry: StompEndpointRegistry
    @Mock lateinit var registration: StompWebSocketEndpointRegistration
    @InjectMocks lateinit var webSocketConfig: WebSocketConfig

    @Test
    fun `should add broker`() {
        /* Given */
        whenever(config.setApplicationDestinationPrefixes(any())).thenReturn(config)
        whenever(config.enableSimpleBroker(any())).thenReturn(null)

        /* When */
        webSocketConfig.configureMessageBroker(config)

        /* Then */
        verify(config, times(1)).setApplicationDestinationPrefixes(ArgumentMatchers.eq("/app"))
        verify(config, times(1)).enableSimpleBroker(ArgumentMatchers.eq("/topic"))
    }

    @Test
    fun `should add endpoint`() {
        /* Given */
        whenever(registration.setAllowedOrigins("*")).thenReturn(registration)
        doAnswer { registration }.whenever(registry).addEndpoint("/ws/sockjs")
        doAnswer { registration }.whenever(registry).addEndpoint("/ws")

        /* When */
        webSocketConfig.registerStompEndpoints(registry)

        /* Then */
        verify(registry, times(1)).addEndpoint(ArgumentMatchers.eq("/ws"))
        verify(registry, times(1)).addEndpoint(ArgumentMatchers.eq("/ws/sockjs"))
    }
}
