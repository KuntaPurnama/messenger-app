package com.app.messenger.model.id;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class ChatParticipantId implements Serializable {
    private String chatId;
    private String phoneNumber;

    // Must override equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatParticipantId that)) return false;
        return Objects.equals(chatId, that.chatId) &&
                Objects.equals(phoneNumber, that.phoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, phoneNumber);
    }
}
