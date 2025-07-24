package com.app.messenger.service;

import com.app.messenger.dto.ContactDTO;
import com.app.messenger.dto.ContactRequestDTO;

import java.util.List;

public interface ContactService {
    void addContact(ContactRequestDTO contactDTO, String phoneNumber);
    List<String> getListOfContact(String userPhoneNumber);
    void removeContact(ContactDTO contactDTO);
}
