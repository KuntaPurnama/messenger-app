package com.app.messenger.service.impl;

import com.app.messenger.dto.ContactDTO;
import com.app.messenger.dto.ContactRequestDTO;
import com.app.messenger.dto.UserDetailResponseDTO;
import com.app.messenger.error.exception.BaseException;
import com.app.messenger.model.ChatParticipant;
import com.app.messenger.model.Contact;
import com.app.messenger.model.User;
import com.app.messenger.repository.ContactRepository;
import com.app.messenger.repository.UserRepository;
import com.app.messenger.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    @Override
    public void addContact(ContactRequestDTO contactDTO, String phoneNumber) {
        if (contactDTO.getContactPhoneNumber().equals(phoneNumber)) {
            throw BaseException.builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("can't add your number to contact")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

        boolean isExists = contactRepository.existsByUserPhoneNumberAndContactPhoneNumber(phoneNumber, contactDTO.getContactPhoneNumber());
        if (isExists) {
            throw BaseException.builder()
                    .code(HttpStatus.CONFLICT.value())
                    .message("this contact already exists")
                    .httpStatus(HttpStatus.CONFLICT)
                    .build();
        }

        boolean isUserExists = userRepository.existsByPhoneNumber(contactDTO.getContactPhoneNumber());
        if (!isUserExists) {
            throw BaseException.builder()
                    .code(HttpStatus.CONFLICT.value())
                    .message("this user does not exist")
                    .httpStatus(HttpStatus.CONFLICT)
                    .build();
        }

        Contact contact = Contact.builder()
                .contactPhoneNumber(contactDTO.getContactPhoneNumber())
                .userPhoneNumber(phoneNumber)
                .build();

        contactRepository.save(contact);
    }

    @Override
    public Page<UserDetailResponseDTO> getListOfContact(Pageable pageable, String userPhoneNumber) {
       List<String> contactPhoneNumbers =  contactRepository.findAllByUserPhoneNumber(userPhoneNumber).stream()
                .map(Contact::getContactPhoneNumber)
                .collect(Collectors.toList());

       return userRepository.findAllByPhoneNumberIn(contactPhoneNumbers, pageable)
                .map(this::convertUserEntityToDTO);
    }

    @Transactional
    @Override
    public void removeContact(ContactDTO contactDTO) {
        boolean isExists = contactRepository.existsByUserPhoneNumberAndContactPhoneNumber(contactDTO.getUserPhoneNumber(), contactDTO.getContactPhoneNumber());
        if (!isExists) {
            throw BaseException.builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("this contact doesn't exists")
                    .httpStatus(HttpStatus.BAD_REQUEST)
                    .build();
        }

        contactRepository.deleteByUserPhoneNumberAndContactPhoneNumber(contactDTO.getUserPhoneNumber(), contactDTO.getContactPhoneNumber());
    }

    private UserDetailResponseDTO convertUserEntityToDTO(User user) {
        UserDetailResponseDTO chatParticipantResponseDTO = new UserDetailResponseDTO();
        BeanUtils.copyProperties(user, chatParticipantResponseDTO);

        return chatParticipantResponseDTO;
    }
}
