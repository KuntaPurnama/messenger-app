package com.app.messenger.model;

import com.app.messenger.dto.enumeration.MessageReactionEnum;
import com.app.messenger.dto.enumeration.MessageStatusEnum;
import com.app.messenger.model.id.MessageActivityId;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@IdClass(MessageActivityId.class)
@Table(name = "message_activities")
public class MessageActivity extends BaseEntity {
    @Id
    @Column(name = "message_id")
    private long messageId;

    @Id
    @Column(name = "user_phone_number")
    private String userPhoneNumber;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private MessageStatusEnum status;

    @Column(name = "reaction")
    private MessageReactionEnum reaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable=false, updatable=false)
    private Message message;
}
