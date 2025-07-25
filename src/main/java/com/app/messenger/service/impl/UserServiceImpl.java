package com.app.messenger.service.impl;

import com.app.messenger.dto.*;
import com.app.messenger.dto.constant.KafkaConstant;
import com.app.messenger.dto.enumeration.NotificationType;
import com.app.messenger.error.exception.BaseException;
import com.app.messenger.model.User;
import com.app.messenger.repository.ContactRepository;
import com.app.messenger.repository.UserRepository;
import com.app.messenger.service.RedisService;
import com.app.messenger.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final ContactRepository contactRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; //5 MB

    private final Path rootDir = Paths.get(System.getProperty("user.dir"));
    private final Path pictureDir = rootDir.resolve("picture");

    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    @Override
    public UserResponseDTO getUserById(String phoneNumber) {
        Optional<User> userOptional = userRepository.findUserByPhoneNumber(phoneNumber);
        if (userOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .message("User with phone number " + phoneNumber + " not found")
                    .build();
        }

        User user = userOptional.get();

        return UserResponseDTO.builder()
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())
                .lastSeenAt(user.getLastSeenAt())
                .build();
    }

    @Override
    public void delete(String phoneNumber) {
        Optional<User> userOptional = userRepository.findUserByPhoneNumber(phoneNumber);
        if (userOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .message("User with phone number " + phoneNumber + " not found")
                    .build();
        }

        userRepository.deleteUserByPhoneNumber(phoneNumber);
    }

    @Override
    public void update(UserRequestDTO userDTO, String phoneNumber) {
        Optional<User> userOptional = userRepository.findUserByPhoneNumber(phoneNumber);
        if (userOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .message("User with phone number " + phoneNumber + " not found")
                    .build();
        }

        User user = userOptional.get();

        if(Objects.nonNull(userDTO.getUsername())) {
            user.setUsername(userDTO.getUsername());
        }

        userRepository.save(user);
    }

    @Transactional
    @Override
    public void updateUserOnlineStatus(String phoneNumber, NotificationType notificationType) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"));
        if (notificationType.equals(NotificationType.USER_OFFLINE)) {
            userRepository.updateUserLastSeen(phoneNumber, now);
        }

        List<String> contactPhoneNumbers = contactRepository.getAllContactByUserPhoneNumberNative(phoneNumber);
        if (CollectionUtils.isEmpty(contactPhoneNumbers)) {
            return;
        }
        List<UserSocketConnectionDTO> userSocketConnectionDTOS = redisService.multiGet(contactPhoneNumbers, UserSocketConnectionDTO.class);

        Map<String, List<NotifyUserEventDTO>> notifyMap = new HashMap<>();

        for (UserSocketConnectionDTO userEventDTO : userSocketConnectionDTOS) {
            NotifyUserEventDTO notifyUserEventDTO = NotifyUserEventDTO.builder()
                    .recipientPhoneNumber(userEventDTO.getPhoneNumber())
                    .senderPhoneNumber(phoneNumber)
                    .type(notificationType)
                    .userLastSeen(now)
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
    public void uploadProfilePicture(MultipartFile file, String phoneNumber) {
        if (Objects.isNull(file)) {
            return;
        }

        Optional<User> userOptional = userRepository.findUserByPhoneNumber(phoneNumber);
        if (userOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .message("User with phone number " + phoneNumber + " not found")
                    .build();
        }

        User user = userOptional.get();

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
            String contentType = file.getContentType();

            if (file.getSize() > MAX_FILE_SIZE) {
                throw BaseException.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .httpStatus(HttpStatus.BAD_REQUEST)
                        .message("File is too large. Max limit is " + MAX_FILE_SIZE)
                        .build();
            }

            Path targetPath;

            if (ALLOWED_IMAGE_TYPES.contains(contentType) && ALLOWED_IMAGE_EXT.contains(extension)) {
                targetPath = pictureDir.resolve(filename);
            } else {
                throw BaseException.builder()
                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .message("File type " + contentType + " not supported")
                        .build();
            }
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            user.setProfileImageUrl(targetPath.toString());
            userRepository.save(user);
        } catch (IOException e) {
            throw BaseException.builder()
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .message("File upload failed: " + e.getMessage())
                    .build();
        }
    }
}
