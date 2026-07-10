package vn.thathinh.util;

import vn.thathinh.model.Conversation;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class ConversationReadUtils {

    private ConversationReadUtils() {}

    public static boolean hasUnread(Conversation conversation, String userId) {
        if (conversation.getLastMessageAt() == null) {
            return false;
        }
        Map<String, Instant> lastRead = conversation.getLastReadAtByUser();
        Instant readAt = lastRead != null ? lastRead.get(userId) : null;
        if (readAt == null) {
            String lastSenderId = conversation.getLastSenderId();
            return lastSenderId != null && !userId.equals(lastSenderId);
        }
        return conversation.getLastMessageAt().isAfter(readAt);
    }

    public static void markRead(Conversation conversation, String userId) {
        Instant readAt = conversation.getLastMessageAt() != null
                ? conversation.getLastMessageAt()
                : Instant.now();
        markReadAt(conversation, userId, readAt);
    }

    public static void markReadAt(Conversation conversation, String userId, Instant readAt) {
        Map<String, Instant> lastRead = conversation.getLastReadAtByUser();
        if (lastRead == null) {
            lastRead = new HashMap<>();
            conversation.setLastReadAtByUser(lastRead);
        }
        lastRead.put(userId, readAt);
    }
}
