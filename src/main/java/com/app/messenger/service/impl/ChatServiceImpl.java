package com.app.messenger.service.impl;

import com.app.messenger.dto.*;
import com.app.messenger.dto.constant.KafkaConstant;
import com.app.messenger.dto.enumeration.MessageReactionEnum;
import com.app.messenger.dto.enumeration.MessageStatusEnum;
import com.app.messenger.dto.enumeration.NotificationType;
import com.app.messenger.error.exception.BaseException;
import com.app.messenger.model.*;
import com.app.messenger.repository.*;
import com.app.messenger.service.ChatService;
import com.app.messenger.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.lang.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final RedisService redisService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public void createChat(ChatRequestDTO chatDTO, String phoneNumber) {
        //check if participant is empty
        if (Collections.isEmpty(chatDTO.getParticipantPhoneNumbers())) {
            throw BaseException.builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("participant can't be empty")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

        //check if participant is not beyond threshold
        if (chatDTO.getParticipantPhoneNumbers().size() > 50) {
            throw BaseException.builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("participant can't be empty")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

        if (!chatDTO.isGroup()){
            //participant in 1o1 chat can't be more than 1.
            if (chatDTO.getParticipantPhoneNumbers().size() > 1){
                throw BaseException.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message("can't have more than 1 participant on non-group chat")
                        .httpStatus(HttpStatus.BAD_REQUEST)
                        .build();
            }

            //check if chat already exists
            String participantPhoneNumber = chatDTO.getParticipantPhoneNumbers().get(0);
            if (chatRepository.isPrivateChatExists(Arrays.asList(phoneNumber, participantPhoneNumber))){
                throw BaseException.builder()
                        .code(HttpStatus.CONFLICT.value())
                        .message("private chat already exists")
                        .httpStatus(HttpStatus.CONFLICT)
                        .build();
            }
        }

        Chat chat = chatRepository.save(Chat.builder()
                .createdBy(phoneNumber)
                .name(chatDTO.getName())
                .isGroup(chatDTO.isGroup())
                .build());


        List<ChatParticipant> chatParticipants = chatDTO.getParticipantPhoneNumbers().stream()
                .map(c -> ChatParticipant.builder()
                        .chatId(chat.getId())
                        .phoneNumber(c).build())
                .collect(Collectors.toList());

        chatParticipants.add(ChatParticipant.builder()
                .chatId(chat.getId())
                .phoneNumber(phoneNumber).build());

        chatParticipantRepository.saveAll(chatParticipants);

        //notify all participants
        List<UserSocketConnectionDTO> userSocketConnectionDTOS = redisService.multiGet(chatDTO.getParticipantPhoneNumbers(), UserSocketConnectionDTO.class);
        Map<String, List<NotifyUserEventDTO>> notifyMap = new HashMap<>();

        for (UserSocketConnectionDTO userEventDTO : userSocketConnectionDTOS) {
            NotifyUserEventDTO notifyUserEventDTO = NotifyUserEventDTO.builder()
                    .chatId(chat.getId())
                    .recipientPhoneNumber(userEventDTO.getPhoneNumber())
                    .senderPhoneNumber(phoneNumber)
                    .type(NotificationType.CHAT)
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
    public void deleteChat(long chatId, String phoneNumber) {
        Optional<Chat> chatOptional = chatRepository.findById(chatId);
        if  (chatOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("chat is not exists")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        Chat chat = chatOptional.get();
        if (!chat.getCreatedBy().equals(phoneNumber)) {
            throw BaseException.builder()
                    .code(HttpStatus.FORBIDDEN.value())
                    .message("not the owner of chat")
                    .httpStatus(HttpStatus.FORBIDDEN)
                    .build();
        }

        chatRepository.deleteById(chatId);
    }

    @Override
    public List<ChatDTO> getListChat(String phoneNumber) {
        List<Long> chatIds = chatParticipantRepository.findAllChatParticipantChatIdNumberByPhoneNumber(phoneNumber);
        return chatRepository.getChatWithParticipantWithIds(chatIds).stream()
                .map(this::convertChatParticipantEntityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getChatParticipants(long chatId, String phoneNumber) {
        //check if phone number part of the participant
        if (!chatParticipantRepository.existsChatParticipantByChatIdAndPhoneNumber(chatId, phoneNumber)) {
            throw BaseException.builder()
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("chat is not exists or phone number is not a participant of the chat")
                    .build();
        }

        return chatParticipantRepository.findAllChatParticipantByChatId(chatId).stream()
                .map(ChatParticipant::getPhoneNumber).collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getListMessage(long chatId, String phoneNumber) {
        //Check if this phone number indeed part of the chat
        if (chatParticipantRepository.existsChatParticipantByChatIdAndPhoneNumber(chatId, phoneNumber)) {
            throw BaseException.builder()
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("chat is not exists or phone number is not a participant of the chat")
                    .build();
        }

        return messageRepository.getListMessageByChatId(chatId).stream()
                .map(this::convertMessageEntityToDTO)
                .collect(Collectors.toList());
    }

    private ChatDTO convertChatParticipantEntityToDTO(Chat chat) {
        ChatDTO chatDTO = new ChatDTO();
        BeanUtils.copyProperties(chat, chatDTO);

        List<String> numberDTOs = chat.getChatParticipants().stream()
                .map(ChatParticipant::getPhoneNumber).collect(Collectors.toList());

        chatDTO.setParticipantPhoneNumbers(numberDTOs);
        return chatDTO;
    }

    private MessageDTO convertMessageEntityToDTO(Message message) {
        MessageDTO messageDTO = new MessageDTO();
        BeanUtils.copyProperties(message, messageDTO);

        List<MessageActivityDTO> messageActivityDTOS = message.getMessageActivities().stream()
                        .map(activity -> {
                            MessageActivityDTO messageActivityDTO = new MessageActivityDTO();
                            BeanUtils.copyProperties(activity, messageActivityDTO);
                            return  messageActivityDTO;
                        })
                        .collect(Collectors.toList());

        List<MessageAttachmentDTO> messageAttachmentDTOS = message.getMessageAttachments().stream()
                        .map(attachment -> {
                            MessageAttachmentDTO messageAttachmentDTO = new MessageAttachmentDTO();
                            BeanUtils.copyProperties(attachment, messageAttachmentDTO);
                            return messageAttachmentDTO;
                        }).collect(Collectors.toList());

        messageDTO.setActivities(messageActivityDTOS);
        messageDTO.setAttachments(messageAttachmentDTOS);

        return messageDTO;
    }
}
