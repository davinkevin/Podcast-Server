package lan.dk.podcastserver.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 12/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSocketConfigTest {

    private @Mock MessageBrokerRegistry config;
    private @Mock StompEndpointRegistry registry;
    private @Mock StompWebSocketEndpointRegistration registration;
    private @InjectMocks WebSocketConfig webSocketConfig;
    
    @Test
    public void should_add_broker() {
        /* Given */
        when(config.setApplicationDestinationPrefixes(anyString())).thenReturn(config);
        when(config.enableSimpleBroker(anyString())).thenReturn(null);

        /* When */
        webSocketConfig.configureMessageBroker(config);

        /* Then */
        verify(config, times(1)).setApplicationDestinationPrefixes(eq("/app"));
        verify(config, times(1)).enableSimpleBroker(eq("/topic"));
    }

    @Test
    public void should_add_endpoint() {
        /* Given */
        when(registry.addEndpoint("/ws/sockjs")).thenReturn(registration);
        when(registration.setAllowedOrigins("*")).thenReturn(registration);
        when(registry.addEndpoint("/ws")).thenReturn(registration);

        /* When */
        webSocketConfig.registerStompEndpoints(registry);

        /* Then */
        verify(registry, times(1)).addEndpoint(eq("/ws"));
        verify(registry, times(1)).addEndpoint(eq("/ws/sockjs"));
    }
}
