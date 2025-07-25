package com.app.messenger.repository;

import com.app.messenger.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserByPhoneNumber(String phoneNumber);
    void deleteUserByPhoneNumber(String phoneNumber);

    @Modifying
    @Query(value = "UPDATE users SET last_seen_at = :now WHERE phone_number = :phoneNumber", nativeQuery = true)
    void updateUserLastSeen(@Param("phoneNumber") String phoneNumber, @Param("now") ZonedDateTime now);

    Page<User> findAllByPhoneNumberIn(List<String> phoneNumber, Pageable pageable);

    List<User> findAllByPhoneNumberIn(List<String> phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}
