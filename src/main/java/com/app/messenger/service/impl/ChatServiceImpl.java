package com.app.messenger.service.impl;

import com.app.messenger.dto.*;
import com.app.messenger.dto.constant.KafkaConstant;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final UserRepository userRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageActivityRepository messageActivityRepository;

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
    public void editChatName(EditChatDTO dto, String phoneNumber) {
        Optional<Chat> chatOptional =  chatRepository.findById(dto.getChatId());
        if (chatOptional.isEmpty()) {
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

        chat.setName(dto.getName());
        chatRepository.save(chat);
    }

    @Override
    public void leaveChat(long chatId, String phoneNumber) {
        Optional<Chat> chatOptional = chatRepository.findById(chatId);
        if (chatOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("chat is not exists")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        Chat chat = chatOptional.get();
        if (!chat.isGroup()) {
            throw BaseException.builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("chat is not group")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

        Optional<ChatParticipant> chatParticipantOptional = chatParticipantRepository.findChatParticipantByChatIdAndPhoneNumber(chatId, phoneNumber);
        if (chatParticipantOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("chat is not exists")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        ChatParticipant chatParticipant = chatParticipantOptional.get();
        chatParticipantRepository.delete(chatParticipant);

//        //notify this user
//        String key = RedisConstant.USER_SOCKET_PREFIX + phoneNumber;
//        UserSocketConnectionDTO userSocketConnectionDTO = redisService.get(key, UserSocketConnectionDTO.class);
//
//        NotifyUserEventDTO userEventDTO = NotifyUserEventDTO.builder()
//                .chatId(chatParticipant.getChatId())
//                .senderPhoneNumber(phoneNumber)
//                .type(NotificationType.UNSUBSCRIBE)
//                .build();
//
//        try {
//            String payload = objectMapper.writeValueAsString(userEventDTO);
//            kafkaTemplate.send(KafkaConstant.KAFKA_USER_NOTIFY_PREFIX + userSocketConnectionDTO.getNodeId(), payload);
//        }catch (Exception e) {
//            log.error("error transform to string for node {}", userSocketConnectionDTO.getNodeId(), e);
//        }
    }

    @Transactional
    @Override
    public void addParticipant(AddParticipantDTO dto) {
        Optional<Chat> chatOptional = chatRepository.findById(dto.getChatId());
        if (chatOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("chat is not exists")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        Chat chat = chatOptional.get();
        if (!chat.isGroup()) {
            throw BaseException.builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("chat is not group")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

        if (!userRepository.existsByPhoneNumber(dto.getParticipantPhoneNumber())) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("participant is not exists")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatId(dto.getChatId())
                .phoneNumber(dto.getParticipantPhoneNumber())
                .build();

        chatParticipantRepository.save(chatParticipant);
    }

    @Override
    public Page<ChatDTO> getListChat(String phoneNumber, Pageable pageable) {
        List<Long> chatIds = chatParticipantRepository.findAllChatParticipantChatIdNumberByPhoneNumber(phoneNumber);
        return chatRepository.getChatWithParticipantWithIds(chatIds, pageable).map(this::convertChatParticipantEntityToDTO);
    }

    @Override
    public Page<UserDetailResponseDTO> getChatParticipants(long chatId, Pageable pageable, String phoneNumber) {
        //check if phone number part of the participant
        if (!chatParticipantRepository.existsChatParticipantByChatIdAndPhoneNumber(chatId, phoneNumber)) {
            throw BaseException.builder()
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("chat is not exists or phone number is not a participant of the chat")
                    .build();
        }
        Page<ChatParticipant>  chatParticipants = chatParticipantRepository.findAllChatParticipantByChatId(chatId, pageable);
        List<String> phoneNumbers = chatParticipants.getContent().stream().map(ChatParticipant::getPhoneNumber).collect(Collectors.toList());

        Map<String, User> userMap = userRepository.findAllByPhoneNumberIn(phoneNumbers).stream()
                .collect(Collectors.toMap(User::getPhoneNumber, user -> user));

        return chatParticipantRepository.findAllChatParticipantByChatId(chatId, pageable)
                .map(s -> convertChatParticipantEntityToDTO(userMap.get(s.getPhoneNumber())));
    }

    @Override
    public Page<MessageDTO> getListMessage(long chatId, Pageable pageable, String phoneNumber) {
        //Check if this phone number indeed part of the chat
        if (!chatParticipantRepository.existsChatParticipantByChatIdAndPhoneNumber(chatId, phoneNumber)) {
            throw BaseException.builder()
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("chat is not exists or phone number is not a participant of the chat")
                    .build();
        }
        Page<Message> messagesPage = messageRepository.findAllByChatId(chatId, pageable);
        if (Objects.isNull(messagesPage) || messagesPage.getTotalElements() == 0) {
            return Page.empty();
        }

        List<Long> messageIds = messagesPage.getContent().stream().map(Message::getId).collect(Collectors.toList());
        Map<Long, List<MessageAttachment>> messageAttachmentMap = messageAttachmentRepository.findByMessageIds(messageIds).stream()
                .collect(Collectors.groupingBy(MessageAttachment::getMessageId));

        Map<Long, List<MessageActivity>> messageActivityMap = messageActivityRepository.findByMessageIds(messageIds).stream()
                .collect(Collectors.groupingBy(MessageActivity::getMessageId));

        Page<MessageDTO> messageDTOS = messagesPage
                .map(m -> {
                    MessageDTO messageDTO = new MessageDTO();
                    BeanUtils.copyProperties(m, messageDTO);

                    if (messageAttachmentMap.containsKey(m.getId()) && !messageAttachmentMap.get(m.getId()).isEmpty()) {
                        List<MessageAttachmentDTO> messageAttachmentDTOs = messageAttachmentMap.get(m.getId()).stream()
                                .map(this::convertAttachmentEntityToDTO)
                                .collect(Collectors.toList());
                        messageDTO.setAttachments(messageAttachmentDTOs);
                    }

                    if (messageActivityMap.containsKey(m.getId()) && !messageActivityMap.get(m.getId()).isEmpty()) {
                        List<MessageActivityDTO> messageActivityDTOS = messageActivityMap.get(m.getId()).stream()
                                .map(this::convertMessageActivityEntityToDTO)
                                .collect(Collectors.toList());
                        messageDTO.setActivities(messageActivityDTOS);
                    }

                    return  messageDTO;
                });

        return messageDTOS;
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

    private UserDetailResponseDTO convertChatParticipantEntityToDTO(User user) {
        UserDetailResponseDTO chatParticipantResponseDTO = new UserDetailResponseDTO();
        BeanUtils.copyProperties(user, chatParticipantResponseDTO);
        chatParticipantResponseDTO.setLastSeenAt(user.getLastSeenAt());

        return chatParticipantResponseDTO;
    }

    private MessageAttachmentDTO convertAttachmentEntityToDTO(MessageAttachment messageAttachment) {
        return MessageAttachmentDTO.builder()
                .id(messageAttachment.getId())
                .messageId(messageAttachment.getMessageId())
                .fileUrl(messageAttachment.getFileUrl())
                .build();
    }

    private MessageActivityDTO convertMessageActivityEntityToDTO(MessageActivity messageActivity) {
        return MessageActivityDTO.builder()
                .id(messageActivity.getMessageId())
                .messageId(messageActivity.getMessageId())
                .status(messageActivity.getStatus())
                .reaction(messageActivity.getReaction())
                .userPhoneNumber(messageActivity.getUserPhoneNumber())
                .build();
    }
}
