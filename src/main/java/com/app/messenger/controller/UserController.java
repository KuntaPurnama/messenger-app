package com.app.messenger.controller;

import com.app.messenger.dto.ResponseDTO;
import com.app.messenger.dto.UserDTO;
import com.app.messenger.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping(name = "/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<UserDTO> login(Principal principal) {
        UserDTO userDTO = userService.getUserById(principal.getName());
        return ResponseDTO.ok(userDTO);
    }

    @DeleteMapping
    public ResponseDTO<Void> deleteUser(Principal principal) {
        userService.delete(principal.getName());
        return ResponseDTO.ok();
    }

    @PutMapping
    public ResponseDTO<Void> updateUser(Principal principal, @RequestBody UserDTO userDTO) {
        userDTO.setPhoneNumber(principal.getName());
        userService.update(userDTO);
        return ResponseDTO.ok();
    }
}
