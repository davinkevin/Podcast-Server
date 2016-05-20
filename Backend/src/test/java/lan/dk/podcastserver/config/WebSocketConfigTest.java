package lan.dk.podcastserver.config;

import lan.dk.podcastserver.utils.jackson.CustomObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 12/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSocketConfigTest {

    @Mock MessageBrokerRegistry config;
    @Mock StompEndpointRegistry registry;
    @Mock StompWebSocketEndpointRegistration registration;
    
    @Test
    public void should_add_broker() {
        /* Given */
        when(config.setApplicationDestinationPrefixes(anyString())).thenReturn(config);
        when(config.enableSimpleBroker(anyString())).thenReturn(null);
        WebSocketConfig webSocketConfig = new WebSocketConfig();

        /* When */
        webSocketConfig.configureMessageBroker(config);

        /* Then */
        verify(config, times(1)).setApplicationDestinationPrefixes(eq("/app"));
        verify(config, times(1)).enableSimpleBroker(eq("/topic"));
    }

    @Test
    public void should_add_endpoint() {
        /* Given */
        when(registry.addEndpoint(anyString())).thenReturn(registration);
        WebSocketConfig webSocketConfig = new WebSocketConfig();

        /* When */
        webSocketConfig.registerStompEndpoints(registry);

        /* Then */
        verify(registry, times(1)).addEndpoint(eq("/ws"));
        verify(registration, times(1)).withSockJS();
    }

    @Test
    public void should_configure_message_converters() {
        /* Given */
        List<MessageConverter> messageConverters = new ArrayList<>();
        WebSocketConfig webSocketConfig = new WebSocketConfig();

        /* When */
        webSocketConfig.configureMessageConverters(messageConverters);

        /* Then */
        assertThat(messageConverters)
                .hasSize(1);
        assertThat(messageConverters.get(0))
                .isOfAnyClassIn(MappingJackson2MessageConverter.class);
        assertThat(((MappingJackson2MessageConverter) messageConverters.get(0)).getObjectMapper())
                .isOfAnyClassIn(CustomObjectMapper.class);
    }
}