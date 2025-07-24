package com.app.messenger.service.impl;

import com.app.messenger.dto.*;
import com.app.messenger.dto.constant.KafkaConstant;
import com.app.messenger.dto.constant.RedisConstant;
import com.app.messenger.dto.enumeration.MessageStatusEnum;
import com.app.messenger.dto.enumeration.NotificationType;
import com.app.messenger.error.exception.BaseException;
import com.app.messenger.model.ChatParticipant;
import com.app.messenger.model.Message;
import com.app.messenger.model.MessageActivity;
import com.app.messenger.model.MessageAttachment;
import com.app.messenger.repository.ChatParticipantRepository;
import com.app.messenger.repository.MessageActivityRepository;
import com.app.messenger.repository.MessageAttachmentRepository;
import com.app.messenger.repository.MessageRepository;
import com.app.messenger.service.MessageService;
import com.app.messenger.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final MessageActivityRepository messageActivityRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final RedisService redisService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.node-id}")
    private String nodeId;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; //5 MB
    private static final String MESSAGE_UPLOAD_PATH = System.getProperty("user.dir") + "/attachment";

    @Transactional
    @Override
    public void sendMessage(MessageRequestDTO dto, String phoneNumber) {
        //check if chat exists or user part of the participant
        Optional<ChatParticipant> chatParticipantOptional = chatParticipantRepository.findChatParticipantByChatIdAndPhoneNumber(dto.getChatId(), phoneNumber);
        if (chatParticipantOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("chat is not exists or  phone number is not a participant of the chat")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        Message message = messageRepository.save(Message.builder()
                .chatId(dto.getChatId())
                .content(dto.getContent())
                .senderPhoneNumber(phoneNumber)
                .build());

        //create default message activity for each participant
        List<String> chatParticipantNumbers = chatParticipantRepository.findAllChatParticipantNumberOnlyByChatId(dto.getChatId());
        List<String> redisKeys = chatParticipantNumbers.stream()
                .map(s -> RedisConstant.USER_SOCKET_PREFIX + s)
                .collect(Collectors.toList());

        List<String> rawList = redisTemplate.opsForValue().multiGet(redisKeys);
        if (Objects.isNull(rawList)) {
            rawList = new ArrayList<>();
        }

        Set<String> onlineUsers = new HashSet<>();
        Set<String> userNodes = new HashSet<>();

        for (String raw: rawList) {
            try {
                UserSocketConnectionDTO userSocketConnectionDTO = objectMapper.readValue(raw, UserSocketConnectionDTO.class);
                onlineUsers.add(userSocketConnectionDTO.getPhoneNumber());
                userNodes.add(userSocketConnectionDTO.getNodeId());
            }catch (JsonProcessingException e){
                log.error("error convert to json");
            }
        }

        List<MessageActivity> messageActivities =  new ArrayList<>();
        for (String number : chatParticipantNumbers) {
            if (number.equals(phoneNumber)) {
                continue;
            }

            MessageStatusEnum messageStatusEnum = onlineUsers.contains(number) ? MessageStatusEnum.DELIVERED : MessageStatusEnum.SENT;
            MessageActivity messageActivity = createDefaultMessageActivityEntity(number, message.getId(), messageStatusEnum);
            messageActivities.add(messageActivity);
        }

        if (Objects.nonNull(dto.getAttachmentURLs())){
            List<MessageAttachment> messageAttachments = dto.getAttachmentURLs().stream()
                    .map(attch -> convertAttachmentDTOToEntity(attch, message.getId()))
                    .collect(Collectors.toList());
            messageAttachments = messageAttachmentRepository.saveAll(messageAttachments);
            message.setMessageAttachments(messageAttachments);
        }

        messageActivities = messageActivityRepository.saveAll(messageActivities);
        message.setMessageActivities(messageActivities);

        MessageEventDTO messageEventDTO = objectMapper.convertValue(message, MessageEventDTO.class);

        try{
            String messageString = objectMapper.writeValueAsString(messageEventDTO);

            for (String node: userNodes) {
                kafkaTemplate.send(KafkaConstant.KAFKA_CHAT_MESSAGE_PREFIX + node, messageString);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }

    @Transactional
    @Override
    public void deleteMessage(MessageDTO messageDTO) {
        //check if message exists
        Optional<Message> messageOptional =  messageRepository.findById(messageDTO.getId());
        if (messageOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("message is not exists")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        //check if user is the owner of message
        Message message = messageOptional.get();
        if (!message.getSenderPhoneNumber().equals(messageDTO.getSenderPhoneNumber())) {
            throw BaseException.builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("sender phone number not the owner of message")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

        messageRepository.deleteById(message.getId());
    }

    @Transactional
    @Override
    public void editMessage(MessageDTO dto) {
        Optional<Message> messageOptional =  messageRepository.findById(dto.getId());
        if (messageOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("message is not exists")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        //check if user is the sender of message
        Message message = messageOptional.get();
        if (!message.getSenderPhoneNumber().equals(dto.getSenderPhoneNumber())) {
            throw BaseException.builder()
                    .code(HttpStatus.FORBIDDEN.value())
                    .message("only sender can edit the message")
                    .httpStatus(HttpStatus.FORBIDDEN)
                    .build();
        }

        message.setContent(dto.getContent());
        messageRepository.save(message);
    }

    @Transactional
    @Override
    public void updateMessageActivity(MessageActivityDTO dto) {
        Optional<MessageActivity> messageActivityOptional = messageActivityRepository.findByMessageId(dto.getId());
        if (messageActivityOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("message not found")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        MessageActivity messageActivity = messageActivityOptional.get();
        messageActivity.setStatus(dto.getStatus());
        messageActivity.setReaction(dto.getReaction());

        messageActivityRepository.save(messageActivity);
    }

    @Override
    public FileUploadDTO uploadFile(List<MultipartFile> files) {
        Map<String, String> errorMap = new HashMap<>();
        List<MessageAttachmentDTO> messageAttachmentDTOS = new ArrayList<>();

        //upload file
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            if (file.getSize() > MAX_FILE_SIZE) {
                errorMap.put(filename, file.getOriginalFilename() + " exceeds max size. Limit is " + MAX_FILE_SIZE);
                continue;
            }

            try {
                Path filePath = Paths.get(MESSAGE_UPLOAD_PATH, filename);
                Files.write(filePath, file.getBytes());

                MessageAttachmentDTO messageAttachmentDTO = new MessageAttachmentDTO();
                messageAttachmentDTO.setFileSize(file.getSize());
                messageAttachmentDTO.setFileType(file.getContentType());
                messageAttachmentDTO.setFileUrl(filePath.toString());

                messageAttachmentDTOS.add(messageAttachmentDTO);
            } catch (IOException e) {
                errorMap.put(filename, file.getOriginalFilename() + " failed to save: " + e.getMessage());
            }
        }

        FileUploadDTO fileUploadDTO = new FileUploadDTO();
        fileUploadDTO.setErrorUploadedFile(errorMap);
        fileUploadDTO.setMessageAttachmentDTOS(messageAttachmentDTOS);

        return fileUploadDTO;
    }

    @Override
    public int countUndeliveredMessage(long chatId, String phoneNumber) {
        if (!chatParticipantRepository.existsChatParticipantByChatIdAndPhoneNumber(chatId, phoneNumber)) {
            return 0;
        }

        return messageRepository.countListUndeliveredMessage(chatId, phoneNumber);
    }

    @Transactional
    @Override
    public List<MessageDTO> getUndeliveredMessage(long chatId, String phoneNumber) {
        if (!chatParticipantRepository.existsChatParticipantByChatIdAndPhoneNumber(chatId, phoneNumber)) {
            return new  ArrayList<>();
        }

        List<MessageDTO> messageDTOS = new ArrayList<>();
        List<Message> messages = messageRepository.getListUndeliveredMessage(chatId, phoneNumber);
        List<Long> messageIds = new ArrayList<>();
        for (Message message : messages) {
            MessageDTO messageDTO = objectMapper.convertValue(message, MessageDTO.class);
            messageDTOS.add(messageDTO);

            messageIds.add(message.getId());
        }

        //update activity to read
        messageActivityRepository.updateMessageActivityStatus(messageIds, phoneNumber, MessageStatusEnum.DELIVERED.name());

        //notify user
        List<String> chatParticipantNumbers = chatParticipantRepository.findAllChatParticipantNumberOnlyByChatId(chatId);
        List<UserSocketConnectionDTO> userSocketConnectionDTOS = redisService.multiGet(chatParticipantNumbers, UserSocketConnectionDTO.class);

        Map<String, List<NotifyUserEventDTO>> notifyMap = new HashMap<>();

        for (UserSocketConnectionDTO userEventDTO : userSocketConnectionDTOS) {
            NotifyUserEventDTO notifyUserEventDTO = NotifyUserEventDTO.builder()
                    .chatId(chatId)
                    .recipientPhoneNumber(userEventDTO.getPhoneNumber())
                    .senderPhoneNumber(phoneNumber)
                    .type(NotificationType.MESSAGE_DELIVERED)
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

        return messageDTOS;
    }

    @Transactional
    @Override
    public void updateMessageActivityStatusToRead(long chatId, String phoneNumber) {
        List<Long> messageIds = messageRepository.getListUnreadMessageId(chatId, phoneNumber);
        messageActivityRepository.updateMessageActivityStatus(messageIds, phoneNumber,  MessageStatusEnum.READ.name());

        //notify user
        List<String> chatParticipantNumbers = chatParticipantRepository.findAllChatParticipantNumberOnlyByChatId(chatId);
        List<UserSocketConnectionDTO> userSocketConnectionDTOS = redisService.multiGet(chatParticipantNumbers, UserSocketConnectionDTO.class);
        Map<String, List<NotifyUserEventDTO>> notifyMap = new HashMap<>();

        for (UserSocketConnectionDTO userEventDTO : userSocketConnectionDTOS) {
            NotifyUserEventDTO notifyUserEventDTO = NotifyUserEventDTO.builder()
                    .chatId(chatId)
                    .recipientPhoneNumber(userEventDTO.getPhoneNumber())
                    .senderPhoneNumber(phoneNumber)
                    .type(NotificationType.MESSAGE_READ)
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
    public void notifyUserTypingStatus(UserTypingStatusDTO dto, String phoneNumber) {
        //notify user
        List<String> chatParticipantNumbers = chatParticipantRepository.findAllChatParticipantNumberOnlyByChatId(dto.getChatId());
        List<UserSocketConnectionDTO> userSocketConnectionDTOS = redisService.multiGet(chatParticipantNumbers, UserSocketConnectionDTO.class);
        Map<String, List<NotifyUserEventDTO>> notifyMap = new HashMap<>();

        for (UserSocketConnectionDTO userEventDTO : userSocketConnectionDTOS) {
            NotifyUserEventDTO notifyUserEventDTO = NotifyUserEventDTO.builder()
                    .chatId(dto.getChatId())
                    .recipientPhoneNumber(userEventDTO.getPhoneNumber())
                    .senderPhoneNumber(phoneNumber)
                    .type(NotificationType.USER_TYPING)
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

    private MessageActivity createDefaultMessageActivityEntity(String participantPhoneNumber, long messageId, MessageStatusEnum messageStatus) {
        return MessageActivity.builder()
                .messageId(messageId)
                .status(messageStatus)
                .userPhoneNumber(participantPhoneNumber)
                .build();
    }

    private MessageAttachment convertAttachmentDTOToEntity(String url, long messageId) {
        return MessageAttachment.builder()
                .messageId(messageId)
                .fileUrl(url)
                .build();
    }
}
