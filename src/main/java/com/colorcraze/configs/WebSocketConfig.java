package com.colorcraze.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration for the application.
 * 
 * Enables STOMP over WebSocket and defines the endpoints and message broker.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Registers the STOMP endpoints that clients will use to connect.
     * This example defines a "/ws" endpoint and allows connections from any origin.
     * It also enables SockJS as a fallback for browsers that do not support WebSocket.
     * 
     * @param registry the STOMP endpoint registry.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Configures the message broker for handling communication.
     * A simple broker with the prefix "/topic" is enabled to send messages to subscribers.
     * Additionally, "/app" is defined as the prefix for messages sent from clients to controllers.
     * 
     * @param registry the message broker registry.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
