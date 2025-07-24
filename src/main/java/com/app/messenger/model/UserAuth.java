package com.app.messenger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "user_auth")
public class UserAuth extends BaseEntity{
    @Id
    private String phoneNumber;

    @Column
    private String password;
}
