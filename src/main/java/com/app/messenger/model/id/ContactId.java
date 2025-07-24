package com.app.messenger.model.id;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class ContactId implements Serializable {
    private String userPhoneNumber;
    private String contactPhoneNumber;

    // Must override equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactId that)) return false;
        return Objects.equals(userPhoneNumber, that.userPhoneNumber) &&
                Objects.equals(contactPhoneNumber, that.contactPhoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userPhoneNumber, contactPhoneNumber);
    }
}
