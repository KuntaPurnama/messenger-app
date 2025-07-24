package com.app.messenger.listener;

import com.app.messenger.dto.MessageEventDTO;
import com.app.messenger.dto.NotifyUserEventDTO;
import com.app.messenger.dto.constant.KafkaConstant;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaConstant.KAFKA_CHAT_MESSAGE_PREFIX + "${app.node-id}",
            groupId = "${app.node-id}"
    )
    public void sendMessageListener(String record) {
        try {
            MessageEventDTO message = objectMapper.readValue(record, MessageEventDTO.class);

            messagingTemplate.convertAndSend(
                    "/topic/chat/" + message.getChatId(),
                    message
            );

            log.info("Delivered message for id {}", message.getId());
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", e.getMessage());
        }
    }

    @KafkaListener(
            topics = KafkaConstant.KAFKA_USER_NOTIFY_PREFIX + "${app.node-id}",
            groupId = "${app.node-id}"
    )
    public void userNotifyListener(String record) {
        try {
            JavaType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, NotifyUserEventDTO.class);
            List<NotifyUserEventDTO> message = objectMapper.readValue(record, listType);

            for (NotifyUserEventDTO notifyUserEventDTO : message) {
                messagingTemplate.convertAndSendToUser(
                        notifyUserEventDTO.getRecipientPhoneNumber(),
                        "/queue/private",
                        message
                );
                log.info("Delivered notification to user {}", notifyUserEventDTO.getRecipientPhoneNumber());
            }
        } catch (Exception e) {
            log.error("Failed to process Kafka message for notification: {}", e.getMessage());
        }
    }
}
