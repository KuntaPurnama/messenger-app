package com.app.messenger.helper.impl;

import com.app.messenger.dto.MessageDTO;
import com.app.messenger.dto.MessageEventDTO;
import com.app.messenger.dto.NotifyUserEventDTO;
import com.app.messenger.dto.UserSocketConnectionDTO;
import com.app.messenger.dto.constant.KafkaConstant;
import com.app.messenger.dto.enumeration.NotificationType;
import com.app.messenger.helper.WebSocketHelper;
import com.app.messenger.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class WebSocketHelperImpl implements WebSocketHelper {
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void forwardMessageEventP2P(List<String> phoneNumbers, Long chatId, String senderPhoneNumber, NotificationType notificationType) {
        List<UserSocketConnectionDTO> userSocketConnectionDTOS = redisService.multiGet(phoneNumbers, UserSocketConnectionDTO.class);
        Map<String, List<NotifyUserEventDTO>> notifyMap = new HashMap<>();

        for (UserSocketConnectionDTO userEventDTO : userSocketConnectionDTOS) {
            NotifyUserEventDTO notifyUserEventDTO = NotifyUserEventDTO.builder()
                    .chatId(chatId)
                    .recipientPhoneNumber(userEventDTO.getPhoneNumber())
                    .senderPhoneNumber(senderPhoneNumber)
                    .type(notificationType)
                    .build();

            if (notifyMap.containsKey(userEventDTO.getNodeId())) {
                List<NotifyUserEventDTO> notifyUserEventDTOS = notifyMap.get(userEventDTO.getNodeId());
                notifyUserEventDTOS.add(notifyUserEventDTO);
            }else {
                List<NotifyUserEventDTO> notifyUserEventDTOS = new LinkedList<>();
                notifyUserEventDTOS.add(notifyUserEventDTO);
                notifyMap.put(userEventDTO.getNodeId(), notifyUserEventDTOS);
            }
        }

        //publish to related node
        for (Map.Entry<String, List<NotifyUserEventDTO>> entry : notifyMap.entrySet()) {
            try {
                String topic = KafkaConstant.KAFKA_USER_NOTIFY_PREFIX + entry.getKey();
                String json =  objectMapper.writeValueAsString(entry.getValue());
                kafkaTemplate.send(topic, json);
            }catch (Exception e) {
                log.error("error transform to string for node {}", entry.getKey(), e);
            }
        }
    }

    @Override
    public void forwardMessageEventBroadcast(MessageEventDTO messageEventDTO, Set<String> userNodes) {
        try{
            String messageString = objectMapper.writeValueAsString(messageEventDTO);

            for (String node: userNodes) {
                kafkaTemplate.send(KafkaConstant.KAFKA_CHAT_MESSAGE_PREFIX + node, messageString);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }
}
