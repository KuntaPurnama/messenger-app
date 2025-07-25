package com.app.messenger.controller;

import com.app.messenger.dto.ResponseDTO;
import com.app.messenger.dto.UserRequestDTO;
import com.app.messenger.dto.UserResponseDTO;
import com.app.messenger.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping( "/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<UserResponseDTO> getUser(Principal principal) {
        UserResponseDTO userDTO = userService.getUserById(principal.getName());
        return ResponseDTO.ok(userDTO);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseDTO<Void> handleUpload(@RequestParam("files") MultipartFile file, Principal principal) {
        userService.uploadProfilePicture(file, principal.getName());
        return ResponseDTO.ok();
    }

    @DeleteMapping
    public ResponseDTO<Void> deleteUser(Principal principal) {
        userService.delete(principal.getName());
        return ResponseDTO.ok();
    }

    @PutMapping
    public ResponseDTO<Void> updateUser(Principal principal, @RequestBody UserRequestDTO userDTO) {
        userService.update(userDTO, principal.getName());
        return ResponseDTO.ok();
    }
}
