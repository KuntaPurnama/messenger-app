package com.app.messenger.service.impl;

import com.app.messenger.dto.*;
import com.app.messenger.dto.constant.RedisConstant;
import com.app.messenger.dto.enumeration.MessageStatusEnum;
import com.app.messenger.dto.enumeration.NotificationType;
import com.app.messenger.error.exception.BaseException;
import com.app.messenger.helper.WebSocketHelper;
import com.app.messenger.model.ChatParticipant;
import com.app.messenger.model.Message;
import com.app.messenger.model.MessageActivity;
import com.app.messenger.model.MessageAttachment;
import com.app.messenger.repository.ChatParticipantRepository;
import com.app.messenger.repository.MessageActivityRepository;
import com.app.messenger.repository.MessageAttachmentRepository;
import com.app.messenger.repository.MessageRepository;
import com.app.messenger.service.MessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final WebSocketHelper webSocketHelper;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; //5 MB
    private final Path rootDir = Paths.get(System.getProperty("user.dir"));
    private final Path pictureDir = rootDir.resolve("picture");
    private final Path videoDir = rootDir.resolve("video");

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4", "video/quicktime", "video/webm", "video/x-msvideo");

    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_VIDEO_EXT = Set.of("mp4", "mov", "webm", "avi");

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
            if (Objects.isNull(raw)) {
                continue;
            }

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

        MessageEventDTO messageEventDTO = new MessageEventDTO();
        BeanUtils.copyProperties(message, messageEventDTO);
        if (Objects.nonNull(dto.getAttachmentURLs())){
            List<MessageAttachment> messageAttachments = dto.getAttachmentURLs().stream()
                    .map(attch -> convertAttachmentDTOToEntity(attch, message.getId()))
                    .collect(Collectors.toList());
            messageAttachments = messageAttachmentRepository.saveAll(messageAttachments);

            messageEventDTO.setAttachments(messageAttachments.stream().map(this::convertAttachmentEntityToDTO).collect(Collectors.toList()));
        }

        messageActivities = messageActivityRepository.saveAll(messageActivities);
        messageEventDTO.setActivities(messageActivities.stream().map(this::convertMessageActivityEntityToDTO).collect(Collectors.toList()));

        //Broadcast event
        webSocketHelper.forwardMessageEventBroadcast(messageEventDTO, userNodes);
    }

    @Transactional
    @Override
    public void deleteMessage(long messageId, String phoneNumber) {
        //check if message exists
        Optional<Message> messageOptional =  messageRepository.findById(messageId);
        if (messageOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("message is not exists")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        //check if user is the owner of message
        Message message = messageOptional.get();
        if (!message.getSenderPhoneNumber().equals(phoneNumber)) {
            throw BaseException.builder()
                    .code(HttpStatus.FORBIDDEN.value())
                    .message("sender phone number not the owner of message")
                    .httpStatus(HttpStatus.FORBIDDEN)
                    .build();
        }

        messageRepository.deleteById(message.getId());
    }

    @Transactional
    @Override
    public void editMessage(UpdateMessageRequestDTO dto, String phoneNumber) {
        Optional<Message> messageOptional =  messageRepository.findById(dto.getMessageId());
        if (messageOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("message is not exists")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

        //check if user is the sender of message
        Message message = messageOptional.get();
        if (!message.getSenderPhoneNumber().equals(phoneNumber)) {
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
        MessageActivity messageActivity = messageActivityRepository.findByMessageIdAndUserPhoneNumber(dto.getMessageId(), dto.getUserPhoneNumber());
        if (Objects.isNull(messageActivity)) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("message not found")
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .build();
        }

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
            try {
                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
                String contentType = file.getContentType();

                if (file.getSize() > MAX_FILE_SIZE) {
                    errorMap.put(filename, file.getOriginalFilename() + " exceeds max size. Limit is 10 MB");
                    continue;
                }

                Path targetPath;

                if (ALLOWED_IMAGE_TYPES.contains(contentType) && ALLOWED_IMAGE_EXT.contains(extension)) {
                    targetPath = pictureDir.resolve(filename);
                } else if (ALLOWED_VIDEO_TYPES.contains(contentType) && ALLOWED_VIDEO_EXT.contains(extension)) {
                    targetPath = videoDir.resolve(filename);
                } else {
                    errorMap.put(filename, file.getOriginalFilename() + " type is not supported");
                    continue;
                }
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                MessageAttachmentDTO messageAttachmentDTO = new MessageAttachmentDTO();
                messageAttachmentDTO.setFileSize(file.getSize());
                messageAttachmentDTO.setFileType(file.getContentType());
                messageAttachmentDTO.setFileUrl(targetPath.toString());

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
    public Page<MessageDTO> getUndeliveredMessage(long chatId, Pageable pageable, String phoneNumber) {
        if (!chatParticipantRepository.existsChatParticipantByChatIdAndPhoneNumber(chatId, phoneNumber)) {
            return Page.empty();
        }

        Page<Message> messagesPage = messageRepository.getListUndeliveredMessage(chatId, phoneNumber, pageable);
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

        //update activity to read
        messageActivityRepository.updateMessageActivityStatus(messageIds, phoneNumber, MessageStatusEnum.DELIVERED.name());

        //notify user
        List<String> chatParticipantNumbers = chatParticipantRepository.findAllChatParticipantNumberOnlyByChatId(chatId);
        webSocketHelper.forwardMessageEventP2P(chatParticipantNumbers, chatId, phoneNumber, NotificationType.MESSAGE_DELIVERED);

        return messageDTOS;
    }

    @Transactional
    @Override
    public void updateMessageActivityStatusToRead(long chatId, String phoneNumber) {
        List<Long> messageIds = messageRepository.getListUnreadMessageId(chatId, phoneNumber);
        messageActivityRepository.updateMessageActivityStatus(messageIds, phoneNumber,  MessageStatusEnum.READ.name());

        //notify user
        List<String> chatParticipantNumbers = chatParticipantRepository.findAllChatParticipantNumberOnlyByChatId(chatId);
        webSocketHelper.forwardMessageEventP2P(chatParticipantNumbers, chatId, phoneNumber, NotificationType.MESSAGE_READ);
    }

    @Override
    public void notifyUserTypingStatus(UserTypingStatusDTO dto, String phoneNumber) {
        //notify user
        List<String> chatParticipantNumbers = chatParticipantRepository.findAllChatParticipantNumberOnlyByChatId(dto.getChatId());
        webSocketHelper.forwardMessageEventP2P(chatParticipantNumbers, dto.getChatId(), phoneNumber, NotificationType.USER_TYPING);
    }

    @Transactional
    @Override
    public void updateMessageReaction(MessageReactionRequestDTO dto, String phoneNumber) {
        Message message = messageRepository.findById(dto.getMessageId()).get();
        if (message.getSenderPhoneNumber().equals(phoneNumber)) {
            throw BaseException.builder()
                    .httpStatus(HttpStatus.CONFLICT)
                    .code(HttpStatus.CONFLICT.value())
                    .message("can't react to your own message")
                    .build();
        }
        MessageActivity messageActivity = messageActivityRepository.findByMessageIdAndUserPhoneNumber(dto.getMessageId(), phoneNumber);

        messageActivity.setReaction(dto.getReaction());
        messageActivity = messageActivityRepository.save(messageActivity);

        //Notify user
        List<String> chatParticipantNumbers = chatParticipantRepository.findAllChatParticipantNumberOnlyByChatId(message.getChatId());
        List<String> redisKeys = chatParticipantNumbers.stream()
                .map(s -> RedisConstant.USER_SOCKET_PREFIX + s)
                .collect(Collectors.toList());
        List<String> rawList = redisTemplate.opsForValue().multiGet(redisKeys);
        if (Objects.isNull(rawList)) {
            rawList = new ArrayList<>();
        }
        Set<String> userNodes = new HashSet<>();

        for (String raw: rawList) {
            try {
                UserSocketConnectionDTO userSocketConnectionDTO = objectMapper.readValue(raw, UserSocketConnectionDTO.class);
                userNodes.add(userSocketConnectionDTO.getNodeId());
            }catch (JsonProcessingException e){
                log.error("error convert to json");
            }
        }

        MessageEventDTO messageEventDTO = new MessageEventDTO();
        BeanUtils.copyProperties(message, messageEventDTO);

        MessageActivityDTO messageActivityDTO = new MessageActivityDTO();
        BeanUtils.copyProperties(messageActivity, messageActivityDTO);

        messageEventDTO.setActivities(List.of(messageActivityDTO));

        //notify user
        webSocketHelper.forwardMessageEventBroadcast(messageEventDTO, userNodes);
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
