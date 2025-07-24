package com.app.messenger.service;

import com.app.messenger.dto.ContactDTO;

import java.util.List;

public interface ContactService {
    void addContact(ContactDTO contactDTO);
    List<String> getListOfContact(String userPhoneNumber);
    void removeContact(ContactDTO contactDTO);
}
