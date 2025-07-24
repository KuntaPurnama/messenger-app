package com.app.messenger.model;

import com.app.messenger.dto.enumeration.UserStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "username")
    private String username;

    @Column(name = "last_seen_at")
    private ZonedDateTime lastSeenAt;
}
