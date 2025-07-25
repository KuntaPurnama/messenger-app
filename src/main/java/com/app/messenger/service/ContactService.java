package com.app.messenger.service;

import com.app.messenger.dto.ContactDTO;
import com.app.messenger.dto.ContactRequestDTO;
import com.app.messenger.dto.UserDetailResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ContactService {
    void addContact(ContactRequestDTO contactDTO, String phoneNumber);
    Page<UserDetailResponseDTO> getListOfContact(Pageable pageable, String userPhoneNumber);
    void removeContact(ContactDTO contactDTO);
}
