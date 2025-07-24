package com.app.messenger.controller;

import com.app.messenger.dto.ContactDTO;
import com.app.messenger.dto.ResponseDTO;
import com.app.messenger.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping( "/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {
    private final ContactService contactService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<Void> addContact(@RequestBody ContactDTO contactDTO, Principal principal) {
        contactDTO.setUserPhoneNumber(principal.getName());
        contactService.addContact(contactDTO);
        return ResponseDTO.ok();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseDTO<List<String>> getContact(Principal principal) {
        List<String> contactList = contactService.getListOfContact(principal.getName());
        return ResponseDTO.ok(contactList);
    }

    @DeleteMapping
    public ResponseDTO<Void> deleteContact(@PathVariable String contactPhoneNumber, Principal principal) {
        ContactDTO contactDTO = ContactDTO.builder()
                .userPhoneNumber(principal.getName())
                .contactPhoneNumber(contactPhoneNumber)
                .build();
        contactService.removeContact(contactDTO);

        return ResponseDTO.ok();
    }
}
