package com.app.messenger.service;

import com.app.messenger.dto.LoginRequestDTO;
import com.app.messenger.dto.LoginResponseDTO;
import com.app.messenger.dto.RegisterRequestDTO;
import com.app.messenger.model.UserAuth;

import java.util.Optional;

public interface UserAuthService {
    Optional<UserAuth> findByPhoneNumber(String phoneNumber);
    LoginResponseDTO login(LoginRequestDTO loginRequestDTO);
    void register(RegisterRequestDTO registerRequestDTO);
}
