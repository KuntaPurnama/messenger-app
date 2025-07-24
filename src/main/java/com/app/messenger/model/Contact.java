package com.app.messenger.model;

import com.app.messenger.model.id.ContactId;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@IdClass(ContactId.class)
@Table(name = "contacts")
public class Contact extends BaseEntity{
    @Id
    @Column(name = "user_phone_number")
    private String userPhoneNumber;

    @Id
    @Column(name = "contact_phone_number")
    private String contactPhoneNumber;
}
