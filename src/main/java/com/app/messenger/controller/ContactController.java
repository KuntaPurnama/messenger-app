package com.app.messenger.controller;

import com.app.messenger.dto.ContactDTO;
import com.app.messenger.dto.ContactRequestDTO;
import com.app.messenger.dto.ResponseDTO;
import com.app.messenger.dto.UserDetailResponseDTO;
import com.app.messenger.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping( "/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {
    private final ContactService contactService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> addContact(@Valid @RequestBody ContactRequestDTO contactDTO, Principal principal) {
        contactService.addContact(contactDTO, principal.getName());
        return ResponseDTO.ok();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Page<UserDetailResponseDTO>> getContact(@ParameterObject
                                                                   @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC)
                                                                   @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                               Principal principal) {
        return ResponseDTO.ok(contactService.getListOfContact(pageable, principal.getName()));
    }

    @DeleteMapping("/{contactPhoneNumber}")
    public ResponseDTO<Void> deleteContact(@PathVariable String contactPhoneNumber, Principal principal) {
        ContactDTO contactDTO = ContactDTO.builder()
                .userPhoneNumber(principal.getName())
                .contactPhoneNumber(contactPhoneNumber)
                .build();
        contactService.removeContact(contactDTO);

        return ResponseDTO.ok();
    }
}
