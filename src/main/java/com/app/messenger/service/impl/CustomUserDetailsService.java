package com.app.messenger.service.impl;

import com.app.messenger.error.exception.BaseException;
import com.app.messenger.model.UserAuth;
import com.app.messenger.repository.UserAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserAuthRepository userAuthRepository;

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) {
        Optional<UserAuth> userAuthOptional = userAuthRepository.findUserAuthByPhoneNumber(phoneNumber);
        if (userAuthOptional.isEmpty()) {
            throw BaseException.builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .message("User with phone number " + phoneNumber + " not found")
                    .build();
        }
        UserAuth userAuth = userAuthOptional.get();
        return User.builder()
                .username(userAuth.getPhoneNumber())
                .password(userAuth.getPassword())
                .build();
    }
}
