package com.app.messenger.model;

import com.app.messenger.model.id.ChatParticipantId;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ChatParticipantId.class)
@Entity
@Builder
@Table(name = "chat_participants")
public class ChatParticipant extends BaseEntity{
    @Id
    @Column(name = "chat_id")
    private long chatId;

    @Id
    @Column(name = "phone_number")
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chat;
}
