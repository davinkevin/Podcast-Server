package lan.dk.podcastserver.config;

import lan.dk.podcastserver.utils.jackson.CustomObjectMapper;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport;

import java.util.List;

//@EnableWebSocketMessageBroker
@Configuration
@ComponentScan(basePackages = {"lan.dk.podcastserver.controller.ws"})
public class WebSocketConfig extends WebSocketMessageBrokerConfigurationSupport {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
          config.setApplicationDestinationPrefixes("/app")
                .enableSimpleBroker("/topic");
    }


    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        //Ajout de la librairie de désierialization spécifiques à Hibernate pour Jackson

        MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter();
        mappingJackson2MessageConverter.setObjectMapper(new CustomObjectMapper());

        return messageConverters.add(mappingJackson2MessageConverter);
    }
}