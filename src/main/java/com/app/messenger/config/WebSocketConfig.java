package com.app.messenger.config;

import com.app.messenger.dto.constant.WebSocketConstant;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(WebSocketConstant.DEFAULT_BROADCAST_TOPIC_SOCKET, WebSocketConstant.QUEUE_TOPIC_SOCKET);
        config.setApplicationDestinationPrefixes(WebSocketConstant.APP_SOCKET);
        config.setUserDestinationPrefix(WebSocketConstant.USER_SOCKET);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
