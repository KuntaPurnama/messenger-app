package com.app.messenger.repository;

import com.app.messenger.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserByPhoneNumber(String phoneNumber);
    void deleteUserByPhoneNumber(String phoneNumber);
}
