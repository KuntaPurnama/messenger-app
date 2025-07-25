package com.app.messenger.controller;

import com.app.messenger.dto.LoginRequestDTO;
import com.app.messenger.dto.LoginResponseDTO;
import com.app.messenger.dto.RegisterRequestDTO;
import com.app.messenger.dto.ResponseDTO;
import com.app.messenger.service.UserAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( "/api/v1/auth")
@RequiredArgsConstructor
public class UserAuthController {
    private final UserAuthService userAuthService;

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        return ResponseDTO.ok(userAuthService.login(loginRequest));
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        userAuthService.register(registerRequestDTO);
        return ResponseDTO.ok();
    }
}
