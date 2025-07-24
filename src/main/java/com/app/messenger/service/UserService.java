package com.app.messenger.service;

import com.app.messenger.dto.UserRequestDTO;
import com.app.messenger.dto.UserResponseDTO;
import com.app.messenger.dto.enumeration.NotificationType;

public interface UserService {

    UserResponseDTO getUserById(String phoneNumber);

    void delete(String phoneNumber);

    void update(UserRequestDTO user, String phoneNumber);

    void updateUserOnlineStatus(String phoneNumber, NotificationType notificationType);
}
