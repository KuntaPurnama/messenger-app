package com.app.messenger.model.id;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class MessageActivityId implements Serializable {
    private long messageId;
    private String userPhoneNumber;

    // Must override equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageActivityId that)) return false;
        return Objects.equals(messageId, that.messageId) &&
                Objects.equals(userPhoneNumber, that.userPhoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, userPhoneNumber);
    }
}
