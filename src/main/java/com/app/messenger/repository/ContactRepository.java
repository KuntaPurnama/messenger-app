package com.app.messenger.repository;

import com.app.messenger.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    void deleteByUserPhoneNumberAndContactPhoneNumber(String userPhoneNumber, String contactPhoneNumber);
    List<Contact> findAllByUserPhoneNumber(String userPhoneNumber);
    boolean existsByUserPhoneNumberAndContactPhoneNumber(String userPhoneNumber, String contactPhoneNumber);
    @Query(value = "SELECT c.contact_phone_number FROM contacts c WHERE c.user_phone_number = :userPhoneNumber", nativeQuery = true)
    List<String> getAllContactByUserPhoneNumberNative(@Param("userPhoneNumber") String userPhoneNumber);
}
