package com.app.messenger.model;

import com.app.messenger.dto.enumeration.UserStatusEnum;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name")
    private String username;

    @Column(name = "phone_number")
    private String phoneNumber;
}
