package com.app.messenger.service.impl;

import com.app.messenger.dto.LoginRequestDTO;
import com.app.messenger.dto.LoginResponseDTO;
import com.app.messenger.dto.RegisterRequestDTO;
import com.app.messenger.dto.UserSocketConnectionDTO;
import com.app.messenger.dto.constant.RedisConstant;
import com.app.messenger.dto.enumeration.NotificationType;
import com.app.messenger.error.exception.BaseException;
import com.app.messenger.model.User;
import com.app.messenger.model.UserAuth;
import com.app.messenger.repository.UserAuthRepository;
import com.app.messenger.repository.UserRepository;
import com.app.messenger.service.JwtService;
import com.app.messenger.service.RedisService;
import com.app.messenger.service.UserAuthService;
import com.app.messenger.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {
    private final JwtService jwtService;
    private final UserAuthRepository userAuthRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;

    @Value("${app.node-id}")
    private String nodeId;


    @Override
    public Optional<UserAuth> findByPhoneNumber(String phoneNumber) {
        return Optional.empty();
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        Optional<UserAuth> userAuthOptional = userAuthRepository.findUserAuthByPhoneNumber(loginRequestDTO.getPhoneNumber());
        if (userAuthOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .httpStatus(HttpStatus.UNAUTHORIZED)
                    .message("Invalid phone number or password")
                    .build();
        }

        UserAuth userAuth = userAuthOptional.get();
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), userAuth.getPassword())) {
            throw BaseException.builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .httpStatus(HttpStatus.UNAUTHORIZED)
                    .message("Invalid phone number or password")
                    .build();
        }

        String key = RedisConstant.USER_SOCKET_PREFIX + loginRequestDTO.getPhoneNumber();
        UserSocketConnectionDTO socketConnectionDTO = UserSocketConnectionDTO.builder()
                .nodeId(nodeId)
                .phoneNumber(loginRequestDTO.getPhoneNumber())
                .build();

        try {
            String value = objectMapper.writeValueAsString(socketConnectionDTO);
            redisService.set(key, value);
            userService.updateUserOnlineStatus(loginRequestDTO.getPhoneNumber(), NotificationType.USER_ONLINE);

            log.info(" connected: userId={}", loginRequestDTO.getPhoneNumber());
        }catch (Exception e){
            throw new RuntimeException("error set value to redis", e);
        }

        return LoginResponseDTO.builder()
                .token(jwtService.generateToken(userAuth.getPhoneNumber()))
                .build();
    }

    @Transactional
    @Override
    public void register(RegisterRequestDTO registerRequestDTO) {
        Optional<UserAuth> userAuthOptional = userAuthRepository.findUserAuthByPhoneNumber(registerRequestDTO.getPhoneNumber());
        if (userAuthOptional.isPresent()) {
            throw BaseException.builder()
                    .code(HttpStatus.UNAUTHORIZED.value())
                    .httpStatus(HttpStatus.UNAUTHORIZED)
                    .message("Phone number already exists")
                    .build();
        }

        //Save user detail
        userRepository.save(User.builder()
                .phoneNumber(registerRequestDTO.getPhoneNumber())
                .username(registerRequestDTO.getUsername())
                .build());

        //save user auth
        userAuthRepository.save(UserAuth.builder()
                .phoneNumber(registerRequestDTO.getPhoneNumber())
                .password(passwordEncoder.encode(registerRequestDTO.getPassword()))
                .build());
    }
}
