package com.app.messenger.service.impl;

import com.app.messenger.dto.NotifyUserEventDTO;
import com.app.messenger.dto.UserDTO;
import com.app.messenger.dto.UserSocketConnectionDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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

    @Override
    public UserDTO getUserById(String phoneNumber) {
        Optional<User> userOptional = userRepository.findUserByPhoneNumber(phoneNumber);
        if (userOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .message("User with phone number " + phoneNumber + " not found")
                    .build();
        }

        User user = userOptional.get();

        return UserDTO.builder()
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
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
    public void update(UserDTO userDTO) {
        Optional<User> userOptional = userRepository.findUserByPhoneNumber(userDTO.getPhoneNumber());
        if (userOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .httpStatus(HttpStatus.NOT_FOUND)
                    .message("User with phone number " + userDTO.getPhoneNumber() + " not found")
                    .build();
        }

        User user = userOptional.get();

        if(Objects.nonNull(userDTO.getUsername())) {
            user.setUsername(userDTO.getUsername());
        }

        userRepository.save(user);
    }

    @Override
    public void updateUserOnlineStatus(String phoneNumber, NotificationType notificationType) {
        List<String> contactPhoneNumbers = contactRepository.getAllContactByUserPhoneNumberNative(phoneNumber);
        List<UserSocketConnectionDTO> userSocketConnectionDTOS = redisService.multiGet(contactPhoneNumbers, UserSocketConnectionDTO.class);

        Map<String, List<NotifyUserEventDTO>> notifyMap = new HashMap<>();

        for (UserSocketConnectionDTO userEventDTO : userSocketConnectionDTOS) {
            NotifyUserEventDTO notifyUserEventDTO = NotifyUserEventDTO.builder()
                    .recipientPhoneNumber(userEventDTO.getPhoneNumber())
                    .senderPhoneNumber(phoneNumber)
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
}
