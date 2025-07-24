package com.app.messenger.service.impl;

import com.app.messenger.dto.ContactDTO;
import com.app.messenger.error.exception.BaseException;
import com.app.messenger.model.Contact;
import com.app.messenger.repository.ContactRepository;
import com.app.messenger.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    @Override
    public void addContact(ContactDTO contactDTO) {
        boolean isExists = contactRepository.existsByUserPhoneNumberAndContactPhoneNumber(contactDTO.getUserPhoneNumber(), contactDTO.getContactPhoneNumber());
        if (isExists) {
            throw BaseException.builder()
                    .code(HttpStatus.CONFLICT.value())
                    .message("this contact already exists")
                    .httpStatus(HttpStatus.CONFLICT)
                    .build();
        }

        Contact contact = Contact.builder()
                .contactPhoneNumber(contactDTO.getContactPhoneNumber())
                .userPhoneNumber(contactDTO.getUserPhoneNumber())
                .build();

        contactRepository.save(contact);
    }

    @Override
    public List<String> getListOfContact(String userPhoneNumber) {
        return contactRepository.findAllByUserPhoneNumber(userPhoneNumber).stream()
                .map(Contact::getContactPhoneNumber)
                .collect(Collectors.toList());
    }

    @Override
    public void removeContact(ContactDTO contactDTO) {
        boolean isExists = contactRepository.existsByUserPhoneNumberAndContactPhoneNumber(contactDTO.getUserPhoneNumber(), contactDTO.getContactPhoneNumber());
        if (isExists) {
            throw BaseException.builder()
                    .code(HttpStatus.CONFLICT.value())
                    .message("this contact already exists")
                    .httpStatus(HttpStatus.CONFLICT)
                    .build();
        }

        contactRepository.deleteByUserPhoneNumberAndContactPhoneNumber(contactDTO.getUserPhoneNumber(), contactDTO.getContactPhoneNumber());
    }
}
