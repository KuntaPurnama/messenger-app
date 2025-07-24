package com.app.messenger.repository;

import com.app.messenger.model.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth,String> {
    Optional<UserAuth> findUserAuthByPhoneNumber(String phoneNumber);
}
