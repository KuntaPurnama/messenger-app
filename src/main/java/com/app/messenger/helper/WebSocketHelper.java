package com.app.messenger.helper;

import com.app.messenger.dto.MessageEventDTO;
import com.app.messenger.dto.enumeration.NotificationType;

import java.util.List;
import java.util.Set;

public interface WebSocketHelper {
    void forwardMessageEventP2P(List<String> phoneNumbers, Long chatId, String senderPhoneNumber, NotificationType notificationType);
    void forwardMessageEventBroadcast(MessageEventDTO messageEventDTO, Set<String> userNodes);
}
