package com.app.messenger.listener;

import com.app.messenger.dto.UserDTO;
import com.app.messenger.dto.UserSocketConnectionDTO;
import com.app.messenger.dto.constant.RedisConstant;
import com.app.messenger.dto.enumeration.NotificationType;
import com.app.messenger.service.RedisService;
import com.app.messenger.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    @Value("${app.node-id}")
    private String nodeId;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (user != null) {
            String phoneNumber = user.getName();
            String key = RedisConstant.USER_SOCKET_PREFIX + phoneNumber;
            UserSocketConnectionDTO socketConnectionDTO = UserSocketConnectionDTO.builder()
                            .nodeId(nodeId)
                            .phoneNumber(phoneNumber)
                            .build();

            try {
                String value = objectMapper.writeValueAsString(socketConnectionDTO);
                redisService.set(key, value);
                userService.updateUserOnlineStatus(phoneNumber, NotificationType.USER_ONLINE);

                log.info(" connected: userId={}, sessionId={}", phoneNumber, sessionId);
            }catch (Exception e){
                throw new RuntimeException("error set value to redis", e);
            }

        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();

        if (user != null) {
            String phoneNumber = user.getName();
            redisService.delete(RedisConstant.USER_SOCKET_PREFIX + phoneNumber);
            userService.updateUserOnlineStatus(phoneNumber, NotificationType.USER_OFFLINE);
            log.info("WebSocket disconnected: userId={}", phoneNumber);
        }
    }
}
