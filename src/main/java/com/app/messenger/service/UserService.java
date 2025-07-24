package com.app.messenger.service;

import com.app.messenger.dto.UserDTO;
import com.app.messenger.dto.enumeration.NotificationType;
import com.app.messenger.dto.enumeration.UserStatusEnum;

public interface UserService {

    UserDTO getUserById(String phoneNumber);

    void delete(String phoneNumber);

    void update(UserDTO user);

    void updateUserOnlineStatus(String phoneNumber, NotificationType notificationType);
}
