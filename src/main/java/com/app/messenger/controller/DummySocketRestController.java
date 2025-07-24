package com.app.messenger.controller;

import com.app.messenger.dto.ResponseDTO;
import com.app.messenger.dto.UserSocketConnectionDTO;
import com.app.messenger.dto.constant.RedisConstant;
import com.app.messenger.dto.enumeration.NotificationType;
import com.app.messenger.service.RedisService;
import com.app.messenger.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/socket")
@RequiredArgsConstructor
public class DummySocketRestController {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    @Value("${app.node-id}")
    private String nodeId;

    @PostMapping("/connect")
    public ResponseDTO<Void> connect(Principal principal) {
        String phoneNumber = principal.getName();
        String key = RedisConstant.USER_SOCKET_PREFIX + phoneNumber;
        UserSocketConnectionDTO socketConnectionDTO = UserSocketConnectionDTO.builder()
                .nodeId(nodeId)
                .phoneNumber(phoneNumber)
                .build();

        try {
            String value = objectMapper.writeValueAsString(socketConnectionDTO);
            redisService.set(key, value);
            userService.updateUserOnlineStatus(phoneNumber, NotificationType.USER_ONLINE);

        }catch (Exception e){
            throw new RuntimeException("error set value to redis", e);
        }

        return ResponseDTO.ok();
    }

    @PostMapping("/disconnect")
    public ResponseDTO<Void> disconnect(Principal principal) {
        String phoneNumber = principal.getName();
        redisService.delete(RedisConstant.USER_SOCKET_PREFIX + phoneNumber);

        userService.updateUserOnlineStatus(phoneNumber, NotificationType.USER_OFFLINE);

        return ResponseDTO.ok();
    }
}
