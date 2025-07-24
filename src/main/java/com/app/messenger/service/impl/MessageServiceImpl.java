package com.app.messenger.service.impl;

import com.app.messenger.dto.*;
import com.app.messenger.dto.constant.KafkaConstant;
import com.app.messenger.dto.enumeration.MessageStatusEnum;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.node-id}")
    private String nodeId;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; //5 MB
    private static final String MESSAGE_UPLOAD_PATH = "/file/attachment";

    @Transactional
    @Override
    public void sendMessage(MessageDTO dto) {
        //check if chat exists or user part of the participant
        Optional<ChatParticipant> chatParticipantOptional = chatParticipantRepository.findChatParticipantByChatIdAndPhoneNumber(dto.getChatId(), dto.getSenderPhoneNumber());
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
                .senderPhoneNumber(dto.getSenderPhoneNumber())
                .build());

        //create default message activity for each participant
        List<ChatParticipant> chatParticipants = chatParticipantRepository.findAllChatParticipantByChatId(dto.getChatId());

        List<String> participantPhoneNumbers = new ArrayList<>();
        List<MessageActivity> messageActivities =  new ArrayList<>();
        for (ChatParticipant chatParticipant : chatParticipants) {
            participantPhoneNumbers.add(chatParticipant.getPhoneNumber());

            if (chatParticipant.getPhoneNumber().equals(dto.getSenderPhoneNumber())) {
                continue;
            }
            MessageActivity messageActivity = createDefaultMessageActivityEntity(chatParticipant.getPhoneNumber(), message.getId());
            messageActivities.add(messageActivity);
        }

        List<MessageAttachment> messageAttachments = dto.getAttachments().stream()
                        .map(attch -> convertAttachmentDTOToEntity(attch, message.getId()))
                        .collect(Collectors.toList());

        messageActivities = messageActivityRepository.saveAll(messageActivities);
        messageAttachments = messageAttachmentRepository.saveAll(messageAttachments);

        //get participant node-connection
        List<UserSocketConnectionDTO> userSocketConnectionDTOS = redisService.multiGet(participantPhoneNumbers, UserSocketConnectionDTO.class);
        Set<String> nodeSet = new HashSet<>();

        message.setMessageActivities(messageActivities);
        message.setMessageAttachments(messageAttachments);

        MessageEventDTO messageEventDTO = objectMapper.convertValue(message, MessageEventDTO.class);

        try{
            String messageString = objectMapper.writeValueAsString(messageEventDTO);

            for (UserSocketConnectionDTO userEventDTO : userSocketConnectionDTOS) {
                if (nodeSet.contains(userEventDTO.getNodeId())) {
                    continue;
                }
                nodeSet.add(userEventDTO.getNodeId());
                kafkaTemplate.send(KafkaConstant.KAFKA_CHAT_MESSAGE_PREFIX + nodeId, messageString);
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
                file.transferTo(filePath.toFile());

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

    @Override
    public List<MessageDTO> getUndeliveredMessage(long chatId, String phoneNumber) {
        if (!chatParticipantRepository.existsChatParticipantByChatIdAndPhoneNumber(chatId, phoneNumber)) {
            return new  ArrayList<>();
        }

        return messageRepository.getListUndeliveredMessage(chatId, phoneNumber).stream()
                .map(m -> objectMapper.convertValue(m, MessageDTO.class))
                .collect(Collectors.toList());
    }

    private MessageActivity createDefaultMessageActivityEntity(String participantPhoneNumber, long messageId) {
        return MessageActivity.builder()
                .messageId(messageId)
                .status(MessageStatusEnum.SENT)
                .userPhoneNumber(participantPhoneNumber)
                .build();
    }

    private MessageAttachment convertAttachmentDTOToEntity(MessageAttachmentDTO dto, long messageId) {
        return MessageAttachment.builder()
                .messageId(messageId)
                .fileSize(dto.getFileSize())
                .fileType(dto.getFileType())
                .fileUrl(dto.getFileUrl())
                .build();
    }
}
