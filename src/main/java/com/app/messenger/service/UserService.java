package com.app.messenger.service;

import com.app.messenger.dto.UserRequestDTO;
import com.app.messenger.dto.UserResponseDTO;
import com.app.messenger.dto.enumeration.NotificationType;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserResponseDTO getUserById(String phoneNumber);

    void delete(String phoneNumber);

    void update(UserRequestDTO user, String phoneNumber);

    void updateUserOnlineStatus(String phoneNumber, NotificationType notificationType);

    void uploadProfilePicture(MultipartFile file, String phoneNumber);
}
